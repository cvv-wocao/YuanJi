# mediapipe_pose.py
import cv2
import mediapipe as mp
import os
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
from mediapipe.tasks.python.core.base_options import BaseOptions

class MediaPipePoseEstimator:
    def __init__(self, model_path=None):
        # 获取当前模块所在目录
        module_dir = os.path.dirname(os.path.abspath(__file__))
        
        # 智能选择模型路径
        if model_path is None:
            # 优先使用轻量级模型（使用绝对路径）
            lite_path = os.path.join(module_dir, "pose_landmarker_lite.task")
            full_path = os.path.join(module_dir, "pose_landmarker.task")
            heavy_path = os.path.join(module_dir, "pose_landmarker_heavy.task")
            
            if os.path.exists(lite_path):
                model_path = lite_path
            elif os.path.exists(full_path):
                model_path = full_path
            else:
                model_path = heavy_path
        else:
            # 如果传递的是相对路径，转换为绝对路径
            if not os.path.isabs(model_path):
                model_path = os.path.join(module_dir, model_path)
        
        print(f"[MediaPipe] 使用模型: {model_path}")
        
        base_options = BaseOptions(model_asset_path=model_path)
        options = vision.PoseLandmarkerOptions(
            base_options=base_options,
            output_segmentation_masks=False,
            num_poses=1,
            # 降低检测阈值，提高召回率
            min_pose_detection_confidence=0.01,
            min_pose_presence_confidence=0.01,
            min_tracking_confidence=0.01
        )
        self.detector = vision.PoseLandmarker.create_from_options(options)

    def estimate(self, frame, bbox=None):
        print(f"[MediaPipe] 收到帧尺寸 {frame.shape}, bbox={bbox}")
        h, w = frame.shape[:2]

        # ========== 限制输入图像的最大尺寸 ==========
        max_dim = 1280
        if w > max_dim or h > max_dim:
            scale = max_dim / max(w, h)
            new_w = int(w * scale)
            new_h = int(h * scale)
            frame = cv2.resize(frame, (new_w, new_h))
            if bbox:
                bbox = [int(v * scale) for v in bbox]
            h, w = frame.shape[:2]
            print(f"[MediaPipe] 帧已缩放到 {w}x{h}")

        offset_x, offset_y = 0, 0  # 初始化偏移量
        roi_w, roi_h = w, h

        if bbox:
            x1, y1, x2, y2 = bbox
            # 扩大 ROI 区域 20%
            width = x2 - x1
            height = y2 - y1
            expand_w = int(width * 0.2)
            expand_h = int(height * 0.2)
            x1 = max(0, x1 - expand_w)
            y1 = max(0, y1 - expand_h)
            x2 = min(w, x2 + expand_w)
            y2 = min(h, y2 + expand_h)
            # 确保边界有效
            if x2 <= x1 or y2 <= y1:
                print("[MediaPipe] 扩大后的 bbox 无效，返回 None")
                return None
            person_roi = frame[y1:y2, x1:x2]
            offset_x, offset_y = x1, y1
            roi_h, roi_w = person_roi.shape[:2]
            print(f"[MediaPipe] 扩大后 ROI 尺寸 {person_roi.shape}")
        else:
            person_roi = frame

        # OpenCV 读取的是 BGR，MediaPipe 需要 RGB，必须先转换
        rgb_roi = cv2.cvtColor(person_roi, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_roi)
        results = self.detector.detect(mp_image)

        if not results.pose_landmarks:
            print("[MediaPipe] 未检测到关键点")
            return None

        print(f"[MediaPipe] 检测到 {len(results.pose_landmarks[0])} 个关键点")

        # 转换坐标回原始图像坐标系（注意：此时原始图像是缩放后的帧，但前端会按实际 canvas 尺寸绘制，归一化坐标有效）
        landmarks = []
        for lm in results.pose_landmarks[0]:
            # lm.x, lm.y 是相对于 ROI 的归一化坐标
            x_abs = lm.x * roi_w + offset_x
            y_abs = lm.y * roi_h + offset_y
            # 转换为缩放后帧的归一化坐标
            class Landmark:
                pass
            landmark = Landmark()
            landmark.x = x_abs / w
            landmark.y = y_abs / h
            landmark.z = lm.z
            landmark.visibility = lm.visibility
            landmarks.append(landmark)
        return landmarks