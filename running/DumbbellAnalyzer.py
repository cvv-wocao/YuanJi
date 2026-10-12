# DumbbellModule.py
import math
import numpy as np
from collections import deque

class AngleSmoother:
    def __init__(self, alpha=0.3):
        self.alpha = alpha
        self.filtered_value = None

    def update(self, value):
        if self.filtered_value is None:
            self.filtered_value = value
        else:
            self.filtered_value = self.alpha * value + (1 - self.alpha) * self.filtered_value
        return self.filtered_value

class KeyPoints:
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_ELBOW = 13
    RIGHT_ELBOW = 14
    LEFT_WRIST = 15
    RIGHT_WRIST = 16
    LEFT_HIP = 23
    RIGHT_HIP = 24

class DumbbellCurlAnalyzer:
    def __init__(self):
        self.standard = {
            "elbow_min": 20,  # 放宽角度区间，从 30 改为 20
            "elbow_max": 160,  # 放宽角度区间，从 150 改为 160
            "elbow_fixed": 0.15
        }
        self.angle_queue = deque(maxlen=10)
        self.state = None
        self.count = 0
        self.lock = False
        self.score = 0
        self.feedback = []
        
        # 添加必要的属性
        self.current_frame = 0
        self.count_lock = False
        self.lock_frames = 0
        self.last_count_frame = 0
        self.min_count_interval = 8  # 减小计数间隔，避免慢动作漏计
        self.prev_landmarks = None
        
        # 添加平滑器
        self.smoother_elbow = AngleSmoother(0.3)

    def detect_movement_robust(self, current, previous):
        if previous is None:
            return True
        try:
            # 只检测上肢关键点的移动
            upper_body_indices = [11, 12, 13, 14, 15, 16]
            movement_threshold = 0.008
            total_movement = 0
            valid_points = 0
            
            for i in upper_body_indices:
                if hasattr(current[i], 'x') and hasattr(previous[i], 'x'):
                    dx = abs(current[i].x - previous[i].x)
                    dy = abs(current[i].y - previous[i].y)
                    total_movement += dx + dy
                    valid_points += 1
            
            if valid_points > 0:
                avg_movement = total_movement / valid_points
                return avg_movement > movement_threshold
            return False
        except:
            return False

    def calculate_angle(self, a, b, c):
        ang = math.degrees(math.atan2(c[1]-b[1], c[0]-b[0]) - math.atan2(a[1]-b[1], a[0]-b[0]))
        return abs(ang) if abs(ang) < 180 else 360-abs(ang)

    def get_pt(self, lm, idx):
        p = lm[idx]
        return (p.x, p.y, p.visibility)

    def analyze(self, lm):
        try:
            self.current_frame +=1
            if self.count_lock:
                self.lock_frames -=1
                if self.lock_frames <=0: self.count_lock=False

            # 只需要上肢，不需要髋部！！！
            ls, le, lw = self.get_pt(lm,11), self.get_pt(lm,13), self.get_pt(lm,15)
            rs, re, rw = self.get_pt(lm,12), self.get_pt(lm,14), self.get_pt(lm,16)

            lv = all([ls, le, lw])
            rv = all([rs, re, rw])

            # 只要一侧手臂清晰就可以识别
            if not lv and not rv:
                return {'angle':0,'count':self.count,'feedback':['未检测到手臂'],'score':0}

            la = self.calculate_angle(ls, le, lw) if lv else 0
            ra = self.calculate_angle(rs, re, rw) if rv else 0
            raw = (la + ra) / 2 if (lv and rv) else (la if lv else ra)

            # 运动检测
            moving = self.detect_movement_robust(lm, self.prev_landmarks)
            angle = self.smoother_elbow.update(raw)
            max_a = self.standard['elbow_max']
            min_a = self.standard['elbow_min']
            fb = []

            if self.state is None:
                # 根据当前肘关节角度智能设置初始状态
                # 角度小于 60° 说明手臂已弯曲到位（顶部）
                # 角度大于 120° 说明手臂较直（准备状态）
                # 中间状态默认为 ready
                if angle > 120:
                    self.state = 'ready'
                elif angle < 60:
                    self.state = 'top'
                else:
                    self.state = 'ready'

            # 计数逻辑
            if self.state == 'ready':
                if angle < max_a - 15:
                    self.state = 'curling'
                    fb = ['弯举中']
            elif self.state == 'curling':
                if angle < min_a + 20:
                    self.state = 'top'
                    fb = ['弯举到位']
            elif self.state == 'top':
                if angle > min_a + 25:
                    self.state = 'lowering'
                    fb = ['下放中']
            elif self.state == 'lowering':
                if angle > max_a - 15:
                    if not self.count_lock and (self.current_frame - self.last_count_frame) > self.min_count_interval:
                        self.count += 1
                        self.last_count_frame = self.current_frame
                        self.count_lock = True
                        self.lock_frames = 12
                        fb = [f"完成一次！总数：{self.count}"]
                    self.state = 'ready'

            s_m = {'ready':'准备','curling':'弯举中','top':'顶部','lowering':'下放中'}
            if not fb:
                fb = [f"{s_m.get(self.state)} 角度:{angle:.1f}°"]

            # 标准判断
            if angle < min_a + 15:
                fb.append("标准姿势")

            # 原评分代码：
            # score = int(np.clip((max_a - angle) / (max_a - min_a) * 100, 0, 100))

            # 改为：
            # 如果有新的计数完成，给高分；否则根据当前阶段给基础分
            if self.count > 0 and self.current_frame - self.last_count_frame < 10:
                score = 100  # 刚完成一次，给满分
            else:
                # 根据肘关节角度偏离理想范围的程度给分
                ideal_min, ideal_max = 30, 150
                if angle < ideal_min:
                    score = int(max(0, 100 - (ideal_min - angle) * 2))
                elif angle > ideal_max:
                    score = int(max(0, 100 - (angle - ideal_max) * 2))
                else:
                    score = 80  # 在范围内给基础分
                score = max(0, min(100, score))

            # 更新之前的关键点
            self.prev_landmarks = lm

            return {
                'angle': round(angle, 1),
                'count': self.count,
                'feedback': fb[:2],
                'score': score
            }
        except Exception as e:
            return {'angle':0,'count':self.count,'feedback':[f'错误：{str(e)}'],'score':0}