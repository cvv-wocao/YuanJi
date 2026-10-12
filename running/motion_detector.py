# motion_detector.py
import numpy as np
from collections import deque

class MotionDetector:
    def __init__(self, threshold=0.001, window_size=3, min_motion_frames=1):
        self.threshold = threshold          # 降低位移阈值，提高灵敏度
        self.window_size = window_size
        self.min_motion_frames = min_motion_frames
        self.prev_landmarks = None
        self.motion_buffer = deque(maxlen=window_size)
        self.is_moving = False

    def update(self, landmarks):
        """
        landmarks: mediapipe 返回的 33 个关键点，每个点有 x, y 坐标（归一化）
        返回当前帧是否运动
        """
        if self.prev_landmarks is None:
            self.prev_landmarks = landmarks
            self.motion_buffer.append(False)
            return False

        # 选取与运动强相关的关键点索引：髋、膝、踝、肩
        key_indices = [11,12, 23,24, 25,26, 27,28]
        total_displacement = 0.0
        count = 0
        for idx in key_indices:
            if idx < len(landmarks) and idx < len(self.prev_landmarks):
                p1 = (landmarks[idx].x, landmarks[idx].y)
                p2 = (self.prev_landmarks[idx].x, self.prev_landmarks[idx].y)
                disp = np.hypot(p1[0]-p2[0], p1[1]-p2[1])
                total_displacement += disp
                count += 1

        avg_displacement = total_displacement / count if count > 0 else 0
        motion = avg_displacement > self.threshold
        self.motion_buffer.append(motion)

        # 连续 min_motion_frames 帧运动才判定为真，避免瞬时抖动
        if sum(self.motion_buffer) >= self.min_motion_frames:
            self.is_moving = True
        else:
            self.is_moving = False

        self.prev_landmarks = landmarks
        return self.is_moving