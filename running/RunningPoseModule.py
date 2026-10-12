#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
跑步姿势检测模块 - YOLOv8+MediaPipe 融合版（带帧间隔优化）
"""

import cv2
import mediapipe as mp
import math
import time
import numpy as np
from collections import deque
import os

try:
    from .fusion_detector import FusionPoseDetector
except ImportError:
    from fusion_detector import FusionPoseDetector


# ========== 保留原有的 KeyPoints, POSE_CONNECTIONS, StandardPoseAnalyzer, UserPoseAnalyzer ==========
# 以下代码与之前版本完全一致，未作修改
class KeyPoints:
    NOSE = 0
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_HIP = 23
    RIGHT_HIP = 24
    LEFT_KNEE = 25
    RIGHT_KNEE = 26
    LEFT_ANKLE = 27
    RIGHT_ANKLE = 28
    LEFT_HEEL = 29
    RIGHT_HEEL = 30
    LEFT_FOOT_INDEX = 31
    RIGHT_FOOT_INDEX = 32


POSE_CONNECTIONS = [
    (11, 12), (11, 13), (13, 15), (12, 14), (14, 16),
    (11, 23), (12, 24), (23, 24),
    (23, 25), (25, 27), (24, 26), (26, 28),
]


class StandardPoseAnalyzer:
    def __init__(self):
        self.standard_data = {
            'knee_angle_range': (120, 175),
            'hip_angle_range': (140, 180),
            'trunk_angle_range': (0, 25),
            'step_width_max': 0.25,
            'cadence_range': (140, 190),
        }

    def calculate_angle(self, a, b, c):
        radians = math.atan2(c[1] - b[1], c[0] - b[0]) - math.atan2(a[1] - b[1], a[0] - b[0])
        angle = abs(radians * 180.0 / math.pi)
        if angle > 180.0: angle = 360 - angle
        return angle

    def get_point(self, landmarks, idx):
        lm = landmarks[idx]
        return (lm.x, lm.y, lm.z, lm.visibility)

    def get_model_path(self, custom_model_path=None):
        if custom_model_path and os.path.exists(custom_model_path):
            return custom_model_path
        model_path = r"D:\program\Face\Run2\Run\pose_landmarker.task"
        if not os.path.exists(model_path):
            model_path = "pose_landmarker.task"
        return model_path


class UserPoseAnalyzer:
    def __init__(self, standard_data=None):
        self.standard_data = standard_data or {
            'knee_angle_range': (120, 175),
            'hip_angle_range': (140, 180),
            'trunk_angle_range': (0, 25),
            'step_width_max': 0.25,
            'cadence_range': (140, 190),
        }
        self.knee_angles = deque(maxlen=30)
        self.hip_angles = deque(maxlen=30)
        self.trunk_angles = deque(maxlen=30)
        self.step_widths = deque(maxlen=30)
        self.ankle_positions = deque(maxlen=50)
        self.movement_positions = deque(maxlen=15)

        self.step_count = 0
        self.last_step_time = 0
        self.step_times = deque(maxlen=10)
        self.cadence_history = deque(maxlen=20)

        self.moving_frames = 0
        self.min_moving_frames = 3

        self.knee_angle = 0
        self.hip_angle = 0
        self.trunk_angle = 0
        self.step_width = 0
        self.score = 0
        self.feedback = []
        self.cadence = 0
        self.is_running = False

    def calculate_angle(self, a, b, c):
        radians = math.atan2(c[1] - b[1], c[0] - b[0]) - math.atan2(a[1] - b[1], a[0] - b[0])
        angle = abs(radians * 180.0 / math.pi)
        if angle > 180.0: angle = 360 - angle
        return angle

    def get_point(self, landmarks, idx):
        lm = landmarks[idx]
        return (lm.x, lm.y, lm.z, lm.visibility)

    def detect_movement(self, la_y, ra_y, lk_y, rk_y):
        current = (la_y, ra_y, lk_y, rk_y)
        self.movement_positions.append(current)
        if len(self.movement_positions) < 5:  # 减少窗口大小
            return False

        total = 0
        for i in range(1, len(self.movement_positions)):
            # 考虑多个关节的运动
            dx1 = abs(self.movement_positions[i][0] - self.movement_positions[i - 1][0])
            dx2 = abs(self.movement_positions[i][1] - self.movement_positions[i - 1][1])
            dy1 = abs(self.movement_positions[i][2] - self.movement_positions[i - 1][2])
            dy2 = abs(self.movement_positions[i][3] - self.movement_positions[i - 1][3])
            total += dx1 + dx2 + dy1 + dy2

        avg = total / (len(self.movement_positions) - 1)
        # 显著降低运动检测阈值
        is_moving = avg > 0.001

        if is_moving:
            self.moving_frames += 1
        else:
            self.moving_frames = max(0, self.moving_frames - 0.5)  # 缓慢减少

        return self.moving_frames >= 2  # 减少所需帧数

    def detect_cadence(self, la, ra):
        t = time.time()
        if len(self.ankle_positions) >= 2:
            pla = self.ankle_positions[-2]
            pra = self.ankle_positions[-1]
            if (la > pla + 0.01 or ra > pra + 0.01) and t - self.last_step_time > 0.3:
                self.step_count += 1
                if self.last_step_time > 0:
                    self.step_times.append(t - self.last_step_time)
                self.last_step_time = t

        self.ankle_positions.append(la)
        if self.step_times:
            return min(220, 60 / (sum(self.step_times) / len(self.step_times)))
        return 0

    def analyze_user_pose(self, lm):
        res = {
            'knee_angle': 0, 'hip_angle': 0, 'trunk_angle': 0, 'step_width': 0,
            'similarity_score': 0, 'feedback': [], 'cadence': 0, 'is_running': False
        }
        try:
            ls = self.get_point(lm, KeyPoints.LEFT_SHOULDER)
            rs = self.get_point(lm, KeyPoints.RIGHT_SHOULDER)
            lh = self.get_point(lm, KeyPoints.LEFT_HIP)
            rh = self.get_point(lm, KeyPoints.RIGHT_HIP)
            lk = self.get_point(lm, KeyPoints.LEFT_KNEE)
            rk = self.get_point(lm, KeyPoints.RIGHT_KNEE)
            la = self.get_point(lm, KeyPoints.LEFT_ANKLE)
            ra = self.get_point(lm, KeyPoints.RIGHT_ANKLE)

            ka = self.calculate_angle(lh, lk, la)
            ha = self.calculate_angle(ls, lh, lk)
            dx = (ls[0] + rs[0]) / 2 - (lh[0] + rh[0]) / 2
            dy = (ls[1] + rs[1]) / 2 - (lh[1] + rh[1]) / 2
            ta = abs(math.atan2(dx, -dy) * 180 / math.pi)
            sw = abs(la[0] - ra[0])

            res['knee_angle'] = ka
            res['hip_angle'] = ha
            res['trunk_angle'] = ta
            res['step_width'] = sw

            moving = self.detect_movement(la[1], ra[1], lk[1], rk[1])
            cadence = self.detect_cadence(la[1], ra[1])
            res['cadence'] = cadence

            if moving or cadence > 80:
                res['is_running'] = True
                res['similarity_score'] = self.get_score(ka, ha, ta, sw, cadence)
                res['feedback'] = self.get_feedback(ka, ha, ta, sw, cadence)
            else:
                res['feedback'] = ["请开始跑步或慢跑"]

            self.knee_angle = ka
            self.hip_angle = ha
            self.trunk_angle = ta
            self.step_width = sw
            self.score = res['similarity_score']
            self.feedback = res['feedback']
            self.cadence = cadence
            self.is_running = res['is_running']

        except:
            res['feedback'] = ["姿势检测中"]
        return res

    def get_score(self, ka, ha, ta, sw, cad):
        std = self.standard_data
        s = 0
        if 120 <= ka <= 175: s += 25
        if 140 <= ha <= 180: s += 25
        if 0 <= ta <= 25: s += 25
        if sw <= 0.25: s += 15
        if 140 <= cad <= 190: s += 10
        return s

    def get_feedback(self, ka, ha, ta, sw, cad):
        fb = []
        if ka < 120:
            fb.append("膝盖可稍伸直")
        elif ka > 175:
            fb.append("膝盖微屈更自然")
        if ta > 25: fb.append("上身略微直立")
        if sw > 0.25: fb.append("步幅可稍小")
        if cad < 140: fb.append("可小幅提高步频")
        if not fb: fb.append("姿势标准自然")
        return fb[:2]


# ========== 优化后的 RunningPoseDetector（带帧间隔检测） ==========
class RunningPoseDetector:
    def __init__(self, min_detection_confidence=0.5, min_tracking_confidence=0.5,
                 custom_model_path=None, detect_interval=3):
        """
        :param detect_interval: 每隔多少帧执行一次完整的 YOLO+MediaPipe 检测
                                设置为 1 表示每帧都检测（最准但最慢）
                                设置为 3 表示每 3 帧检测一次（推荐，性能提升明显）
        """
        self.standard_analyzer = StandardPoseAnalyzer()
        self.standard_data = self.standard_analyzer.standard_data
        self.user_analyzer = UserPoseAnalyzer(self.standard_data)



        # ========== 智能定位 YOLO 模型路径 ==========
        if custom_model_path and os.path.exists(custom_model_path):
            yolo_path = custom_model_path
        else:
            # 获取当前文件所在目录（running/）
            current_dir = os.path.dirname(os.path.abspath(__file__))
            # 获取项目根目录（running/ 的上一级）
            project_root = os.path.dirname(current_dir)
            # 拼接 YOLO 文件夹下的 best.pt
            yolo_path = os.path.join(project_root, "YOLO", "best.pt")

            # 如果上述路径不存在，再尝试当前目录下的 best.pt
            if not os.path.exists(yolo_path):
                yolo_path = "best.pt"

        print(f"🔍 正在加载 YOLO 模型: {yolo_path}")  # 调试输出
        self.fusion_detector = FusionPoseDetector(yolo_model_path=yolo_path, conf_threshold=0.2  )

        self.frame_counter = 0
        self.detect_interval = detect_interval
        self.last_result = None

        # 融合检测器
        # 获取当前文件所在目录
        current_dir = os.path.dirname(os.path.abspath(__file__))
        # 项目根目录（running 目录的上级）
        project_root = os.path.dirname(current_dir)
        yolo_path =  os.path.join(project_root, "YOLO", "best.pt")
        self.fusion_detector = FusionPoseDetector(yolo_model_path=yolo_path)

        # 帧间隔优化相关变量
        self.frame_counter = 0  # 累计帧计数
        self.detect_interval = detect_interval  # 检测间隔
        self.last_result = None  # 上一次检测的结果（用于复用）

    def find_pose(self, img, draw=True):
        """
        输入 BGR 图像，返回 (img, results)
        如果 draw=True，会在图像上绘制检测框和关键点（可选）
        """
        self.frame_counter += 1

        # 判断是否需要执行完整检测
        if self.frame_counter % self.detect_interval == 0:
            # 执行融合检测
            result = self.fusion_detector.detect(img)
            if result is not None:
                self.last_result = result
        else:
            # 复用上一次的检测结果（如果存在）
            result = self.last_result

        # 如果一直没有检测到人体，result 可能为 None
        if result is None:
            return img, None

        # 可选：绘制关键点和 bbox（根据需要实现）
        if draw and hasattr(result, 'pose_landmarks') and result.pose_landmarks:
            # 获取第一组关键点
            landmarks = result.pose_landmarks[0]
            h, w = img.shape[:2]
            for lm in landmarks:
                x = int(lm.x * w)
                y = int(lm.y * h)
                cv2.circle(img, (x, y), 3, (0, 255, 0), -1)
            # 绘制骨骼连线（使用 POSE_CONNECTIONS）
            for connection in POSE_CONNECTIONS:
                start_idx, end_idx = connection
                if start_idx < len(landmarks) and end_idx < len(landmarks):
                    start = landmarks[start_idx]
                    end = landmarks[end_idx]
                    if start.visibility > 0.5 and end.visibility > 0.5:
                        sx, sy = int(start.x * w), int(start.y * h)
                        ex, ey = int(end.x * w), int(end.y * h)
                        cv2.line(img, (sx, sy), (ex, ey), (0, 255, 0), 2)

        return img, result

    def analyze_video(self, path):
        cap = cv2.VideoCapture(path)
        self.user_analyzer = UserPoseAnalyzer(self.standard_data)
        scores = []
        while cap.isOpened():
            ret, f = cap.read()
            if not ret: break
            _, res = self.find_pose(f)
            if res.pose_landmarks:
                for lm in res.pose_landmarks:
                    r = self.user_analyzer.analyze_user_pose(lm)
                    scores.append(r['similarity_score'])
        cap.release()
        avg = np.mean(scores) if scores else 0
        return f"跑步评分：{avg:.1f} 分", None