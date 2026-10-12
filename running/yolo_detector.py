# yolo_detector.py
from ultralytics import YOLO

class YOLOHumanDetector:
    def __init__(self, model_path="best.pt", conf_threshold=0.1):
        self.model = YOLO(model_path)
        self.conf_threshold = conf_threshold

    def detect(self, frame):
        results = self.model(frame, verbose=False)
        human_detections = []
        for result in results:
            if result.boxes is None:
                continue
            for box in result.boxes:
                if int(box.cls) == 0:  # person class
                    x1, y1, x2, y2 = box.xyxy[0].cpu().numpy()
                    conf = float(box.conf[0].cpu().numpy())
                    if conf > self.conf_threshold:
                        human_detections.append({
                            "bbox": [int(x1), int(y1), int(x2), int(y2)],
                            "confidence": conf
                        })
        if human_detections:
            return max(human_detections, key=lambda x: x["confidence"])
        return None