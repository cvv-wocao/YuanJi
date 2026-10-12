# fusion_detector.py
try:
    from .yolo_detector import YOLOHumanDetector
    from .mediapipe_pose import MediaPipePoseEstimator
except ImportError:
    from yolo_detector import YOLOHumanDetector
    from mediapipe_pose import MediaPipePoseEstimator

class FusionPoseDetector:
    def __init__(self, yolo_model_path="best.pt", mediapipe_model_path="pose_landmarker_lite.task", conf_threshold=0.2):
        """
        :param yolo_model_path: YOLO 模型路径
        :param mediapipe_model_path: MediaPipe 模型路径
        :param conf_threshold: YOLO 检测置信度阈值
        """
        self.yolo = YOLOHumanDetector(yolo_model_path, conf_threshold=conf_threshold)
        self.pose = MediaPipePoseEstimator(mediapipe_model_path)

    def detect(self, frame):
        human = self.yolo.detect(frame)
        if human is None:
            # 如果YOLO检测失败，尝试直接使用整个帧
            landmarks = self.pose.estimate(frame, None)
            if landmarks is None:
                return None

            class PoseResult:
                pass

            result = PoseResult()
            result.pose_landmarks = [landmarks]
            result.detection_confidence = 0.5  # 默认置信度
            return result

        x1, y1, x2, y2 = human["bbox"]
        h, w = frame.shape[:2]

        # 更合理的ROI扩大策略
        width = x2 - x1
        height = y2 - y1
        expand_w = int(width * 0.3)  # 适度扩大
        expand_h = int(height * 0.3)  # 适度扩大

        x1 = max(0, x1 - expand_w)
        y1 = max(0, y1 - expand_h)
        x2 = min(w, x2 + expand_w)
        y2 = min(h, y2 + expand_h)

        # 确保边界有效
        if x2 <= x1 or y2 <= y1:
            # 如果边界无效，使用整个帧
            landmarks = self.pose.estimate(frame, None)
        else:
            landmarks = self.pose.estimate(frame, (x1, y1, x2, y2))

        if landmarks is None:
            return None

        class PoseResult:
            pass

        result = PoseResult()
        result.pose_landmarks = [landmarks]
        result.detection_confidence = human["confidence"]
        return result