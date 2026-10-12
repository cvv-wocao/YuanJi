# PushUpModule.py
import math
import numpy as np
from collections import deque

class KeyPoints:
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_HIP = 23
    RIGHT_HIP = 24
    LEFT_ELBOW = 13
    RIGHT_ELBOW = 14
    LEFT_WRIST = 15
    RIGHT_WRIST = 16

class PushUpAnalyzer:
    def __init__(self):
        self.standard = {
            "elbow_down": 60,   # 标准下压角度
            "elbow_up": 150,    # 标准撑起角度
            "body_straight": 0.25  # 躯干直线度阈值
        }
        self.elbow_angles = deque(maxlen=10)  # 角度平滑，保留
        self.prev_angle = None  # 上一帧的肘关节角度，用于运动检测

    def calculate_angle(self, a, b, c):
        ang = math.degrees(math.atan2(c[1]-b[1], c[0]-b[0]) - math.atan2(a[1]-b[1], a[0]-b[0]))
        return abs(ang) if abs(ang) < 180 else 360-abs(ang)

    def get_pt(self, lm, idx):
        p = lm[idx]
        return (p.x, p.y, p.visibility)

    def analyze(self, lm):
        try:
            # 保留原有关键点提取+角度计算
            ls, le, lw = self.get_pt(lm,11), self.get_pt(lm,13), self.get_pt(lm,15)
            rs, re, rw = self.get_pt(lm,12), self.get_pt(lm,14), self.get_pt(lm,16)
            lh, rh = self.get_pt(lm,23), self.get_pt(lm,24)
            angle_l = self.calculate_angle(ls, le, lw)
            angle_r = self.calculate_angle(rs, re, rw)
            angle = (angle_l + angle_r) / 2
            self.elbow_angles.append(angle)
            avg_angle = np.mean(self.elbow_angles)

            # ========== 运动检测（基于肘关节角度的帧间变化） ==========
            if self.prev_angle is not None:
                angle_change = abs(avg_angle - self.prev_angle)
                if angle_change < 3:   # 角度变化小于3度，认为静止
                    moving = False
                else:
                    moving = True
            else:
                moving = True
            self.prev_angle = avg_angle

            if not moving:
                return {
                    "angle": round(avg_angle, 1),
                    "score": 0,
                    "feedback": ["请开始俯卧撑"],
                    "trunkAngle": 0
                }

            # 核心判断：躯干直线度（肩-髋y轴差）+ 肘部角度
            body_straight = abs((ls[1]+rs[1])/2 - (lh[1]+rh[1])/2)
            is_body_straight = body_straight < self.standard["body_straight"]
            is_elbow_normal = self.standard["elbow_down"] <= avg_angle <= self.standard["elbow_up"]

            # 反馈+伤病+建议映射
            feedback = []
            disease_risk = []
            fix_suggest = []
            if not is_elbow_normal:
                feedback.append(f"肘部角度异常（当前{avg_angle:.1f}°，标准70-120°）")
                disease_risk.append("肘关节损伤、胸肌拉伤")
                fix_suggest.append("控制下压深度，肘部与身体呈45°角，避免过度下压")
            if not is_body_straight:
                feedback.append("身体弯曲（塌腰/撅臀）")
                disease_risk.append("腰椎间盘突出、腰肌劳损、下背部疼痛")
                fix_suggest.append("收紧核心，保持肩、髋、膝盖在同一直线")

            # 评分：无问题=100，单问题=75，双问题=50
            score = 100 if (is_elbow_normal and is_body_straight) else 75 if (is_elbow_normal or is_body_straight) else 50

            if not feedback:
                feedback = ["俯卧撑动作标准"]
                disease_risk = ["无"]
                fix_suggest = ["保持核心收紧，匀速上下发力"]

            return {
                "angle": round(avg_angle, 1),
                "trunkAngle": round(body_straight * 100, 1),  # 转换为度数或比例
                "score": score,
                "feedback": feedback[:2],
                "disease_risk": list(set(disease_risk))[:2],
                "fix_suggest": fix_suggest[:2]
            }
        except:
            return {
                "angle": 0,
                "score": 0,
                "feedback": ["请全身入镜，保持姿势"],
                "disease_risk": "无",
                "fix_suggest": "调整拍摄角度，让全身进入画面"
            }