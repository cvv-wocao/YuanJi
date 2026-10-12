# ExerciseAnalyzer.py
import cv2
import math
import numpy as np
import time
import os
import pickle
from collections import deque

# ========== 导入三个专业识别模块 ==========
try:
    # 作为包导入时使用相对导入
    from .RunningPoseModule import RunningPoseDetector, UserPoseAnalyzer
    from .PushUpAnalyzer import PushUpAnalyzer
    from .DumbbellAnalyzer import DumbbellCurlAnalyzer
    from .motion_detector import MotionDetector
except ImportError:
    # 直接运行时使用绝对导入
    from RunningPoseModule import RunningPoseDetector, UserPoseAnalyzer
    from PushUpAnalyzer import PushUpAnalyzer
    from DumbbellAnalyzer import DumbbellCurlAnalyzer
    from motion_detector import MotionDetector

# ========== 轻量分类模型相关导入 ==========
try:
    import lightgbm as lgb
    LGB_AVAILABLE = True
except ImportError:
    LGB_AVAILABLE = False
    print("警告: LightGBM 未安装，姿态分类功能不可用。如需使用请运行: pip install lightgbm")


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


class ExerciseAnalyzer:
    def __init__(self, lgb_model_path="pose_classifier.txt"):
        # ========== 三个专业模块 ==========
        self.running_detector = RunningPoseDetector()
        self.pushup_detector = PushUpAnalyzer()
        self.dumbbell_detector = DumbbellCurlAnalyzer()

        self.exercise_type = "running"
        self.counter = 0

        self.standard_data = {
            'bicep_curl': {'angle_range': (30, 160), 'angle_mean': 95},
            'pushup': {'angle_range': (60, 160), 'angle_mean': 110},
            'running': {
                'knee': (130, 170),
                'hip': (120, 165),
                'trunk': (0, 15),
                'step_width': (0.15, 0.35)
            }
        }

        self.smoother_elbow = AngleSmoother(0.3)
        self.smoother_knee = AngleSmoother(0.3)
        self.smoother_hip = AngleSmoother(0.3)
        self.smoother_trunk = AngleSmoother(0.3)

        self.stage = None
        self.count_lock = False
        self.lock_frames = 0
        self.last_count_frame = 0
        self.min_count_interval = 12
        self.current_frame = 0

        self.prev_landmarks = None
        self.static_frames = 0
        self.moving_flag = False

        # ========== 运动检测器 ==========
        self.motion_detector = MotionDetector(threshold=0.012, min_motion_frames=3)

        # ========== 初始化 LightGBM 分类器 ==========
        self.lgb_model = None
        if os.path.exists(lgb_model_path):
            try:
                # 使用pickle加载训练好的模型
                with open(lgb_model_path, 'rb') as f:
                    self.lgb_model = pickle.load(f)
                print(f"✅ 成功加载 LightGBM 模型: {lgb_model_path}")
            except Exception as e:
                print(f"⚠️ 加载 LightGBM 模型失败: {e}")
        else:
            print(f"⚠️ LightGBM 模型文件不存在: {lgb_model_path}，姿态分类功能禁用")

    # ========== 切换运动时重置 ==========
    def set_exercise_type(self, t):
        self.exercise_type = t
        self.counter = 0
        self.stage = None
        self.count_lock = False
        self.current_frame = 0
        self.prev_landmarks = None

        # 重置高精度模块
        if t == "pushup":
            self.pushup_detector = PushUpAnalyzer()
        elif t == "bicep_curl" or t == "dumbbell_curl":
            self.dumbbell_detector = DumbbellCurlAnalyzer()
        # 重置运动检测器，使用更宽松的阈值
        if t == "running":
            self.motion_detector = MotionDetector(threshold=0.0005, min_motion_frames=1)
        elif t == "dumbbell_curl":
            self.motion_detector = MotionDetector(threshold=0.0005, min_motion_frames=1)
        else:
            self.motion_detector = MotionDetector(threshold=0.0005, min_motion_frames=1)

    def _calc_angle(self, a, b, c):
        """计算三点夹角（度），a, b, c 为 Landmark 对象，b 为顶点"""
        ba = np.array([a.x - b.x, a.y - b.y, a.z - b.z])
        bc = np.array([c.x - b.x, c.y - b.y, c.z - b.z])
        cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-6)
        angle = np.arccos(np.clip(cosine, -1.0, 1.0))
        return math.degrees(angle)

    def _extract_geometric_features(self, landmarks):
        """
        从 landmarks 中提取几何特征（共 12 维）：
        - 躯干倾角（相对于水平面，1维）
        - 左右肘关节角度（2维）
        - 左右膝关节角度（2维）
        - 左右肩-髋-膝夹角（2维）
        - 左右髋-膝-踝夹角（2维）
        - 身体垂直比例（肩到髋的垂直距离 / 髋到踝的垂直距离）（1维）
        - 肩宽与髋宽之比（判断身体旋转）（1维）
        - 身体垂直伸展度（1维）
        返回 list of float
        """
        feats = []
        # 定义关键点索引（MediaPipe 33个关键点）
        # 左肩11, 右肩12, 左肘13, 右肘14, 左腕15, 右腕16
        # 左髋23, 右髋24, 左膝25, 右膝26, 左踝27, 右踝28
        try:
            # 1. 躯干倾角（肩中点到髋中点连线与水平面的夹角）
            l_shoulder = landmarks[11]
            r_shoulder = landmarks[12]
            l_hip = landmarks[23]
            r_hip = landmarks[24]
            shoulder_center = np.array([(l_shoulder.x + r_shoulder.x) / 2, (l_shoulder.y + r_shoulder.y) / 2])
            hip_center = np.array([(l_hip.x + r_hip.x) / 2, (l_hip.y + r_hip.y) / 2])
            delta = hip_center - shoulder_center
            trunk_angle = math.degrees(math.atan2(delta[1], delta[0]))  # 与水平面夹角
            feats.append(trunk_angle)

            # 2. 左右肘关节角度
            left_elbow = self._calc_angle(landmarks[11], landmarks[13], landmarks[15])
            right_elbow = self._calc_angle(landmarks[12], landmarks[14], landmarks[16])
            feats.extend([left_elbow, right_elbow])

            # 3. 左右膝关节角度
            left_knee = self._calc_angle(landmarks[23], landmarks[25], landmarks[27])
            right_knee = self._calc_angle(landmarks[24], landmarks[26], landmarks[28])
            feats.extend([left_knee, right_knee])

            # 4. 左右肩-髋-膝夹角（判断躯干折叠程度）
            left_shoulder_hip_knee = self._calc_angle(landmarks[11], landmarks[23], landmarks[25])
            right_shoulder_hip_knee = self._calc_angle(landmarks[12], landmarks[24], landmarks[26])
            feats.extend([left_shoulder_hip_knee, right_shoulder_hip_knee])

            # 5. 左右髋-膝-踝夹角
            left_hip_knee_ankle = self._calc_angle(landmarks[23], landmarks[25], landmarks[27])
            right_hip_knee_ankle = self._calc_angle(landmarks[24], landmarks[26], landmarks[28])
            feats.extend([left_hip_knee_ankle, right_hip_knee_ankle])

            # 6. 身体垂直比例（肩到髋的垂直距离 / 髋到踝的垂直距离）
            shoulder_y = (landmarks[11].y + landmarks[12].y) / 2
            hip_y = (landmarks[23].y + landmarks[24].y) / 2
            ankle_y = (landmarks[27].y + landmarks[28].y) / 2
            trunk_height = abs(shoulder_y - hip_y)
            leg_height = abs(hip_y - ankle_y)
            vertical_ratio = trunk_height / (leg_height + 1e-6)
            feats.append(vertical_ratio)

            # 7. 肩宽与髋宽之比（判断身体旋转）
            shoulder_width = abs(landmarks[11].x - landmarks[12].x)
            hip_width = abs(landmarks[23].x - landmarks[24].x)
            width_ratio = shoulder_width / (hip_width + 1e-6)
            feats.append(width_ratio)

            # 8. 身体垂直伸展度（额外添加一维特征，确保总维度为12）
            shoulder_y = (landmarks[11].y + landmarks[12].y) / 2
            ankle_y = (landmarks[27].y + landmarks[28].y) / 2
            body_extension = abs(shoulder_y - ankle_y)
            feats.append(body_extension)

        except Exception as e:
            # 如果任何关键点缺失，用0填充（保持特征维度固定）
            print(f"几何特征提取失败: {e}")
            feats = [0.0] * 12  # 总共12维
        return feats


    # ========== 特征提取函数（供 LightGBM 使用） ==========
    def _extract_landmark_features(self, landmarks):
        """
        从 MediaPipe 33 个关键点中提取特征向量
        每个关键点包含 x, y, z, visibility 共 4 个值 -> 33*4 = 132 维
        再拼接几何特征（12维） -> 总特征维度 144
        """
        # 原有基础特征
        base_features = []
        for lm in landmarks:
            base_features.extend([lm.x, lm.y, lm.z, lm.visibility])

        # 新增几何特征
        geo_features = self._extract_geometric_features(landmarks)

        # 合并
        all_features = base_features + geo_features
        return np.array(all_features)

    def _extract_running_angles(self, landmarks):
        """提取跑步相关的角度特征（供 LightGBM 使用）"""
        try:
            a = UserPoseAnalyzer()
            shoulder = a.get_point(landmarks, 11)
            hip = a.get_point(landmarks, 23)
            knee = a.get_point(landmarks, 25)
            ankle = a.get_point(landmarks, 27)
            if all([shoulder, hip, knee, ankle]):
                knee_angle = a.calculate_angle(hip, knee, ankle)
                hip_angle = a.calculate_angle(shoulder, hip, knee)
                trunk_angle = abs(shoulder[1] - hip[1]) * 100
                return [knee_angle, hip_angle, trunk_angle]
        except:
            pass
        return [0, 0, 0]

    def _classify_pose(self, landmarks):
        """
        使用 LightGBM 对当前姿态进行分类
        返回 (类别名称, 置信度)
        """
        if self.lgb_model is None:
            print("  [分类] 模型未加载，返回 None")
            return None, 0.0

        try:
            # 提取完整特征（包括几何特征）
            features = self._extract_landmark_features(landmarks)
            features = features.reshape(1, -1)
            print(f"  [分类] 特征维度: {features.shape}")
            
            # 为了避免特征名称警告，使用 pandas DataFrame 包装特征
            try:
                import pandas as pd
                # 生成特征名称（与训练时一致）
                feature_names = [f'lm_{i}_{dim}' for i in range(33) for dim in ['x', 'y', 'z', 'visibility']]
                feature_names += [f'geo_{i}' for i in range(12)]
                features_df = pd.DataFrame(features, columns=feature_names)
                pred_proba = self.lgb_model.predict_proba(features_df)[0]
            except ImportError:
                # 如果没有 pandas，直接使用 numpy 数组（会有警告但不影响功能）
                pred_proba = self.lgb_model.predict_proba(features)[0]
            class_id = np.argmax(pred_proba)
            confidence = float(pred_proba[class_id])
            
            print(f"  [分类] 预测概率: {pred_proba}")
            print(f"  [分类] 类别ID: {class_id}, 置信度: {confidence:.4f}")

            # 类别映射（根据训练标签定义）
            class_names = {0: "running", 1: "dumbbell_curl", 2: "pushup"}
            class_name = class_names.get(class_id, f"class_{class_id}")
            print(f"  [分类] 分类结果: {class_name}")
            return class_name, confidence
        except Exception as e:
            print(f"  [分类] LightGBM 预测失败: {e}")
            import traceback
            traceback.print_exc()
            return None, 0.0

    # ========== 动作预校验 ==========
    def _validate_action_type(self, landmarks):
        """根据当前运动类型，检查关键点是否满足基本几何条件"""
        if self.exercise_type == "pushup":
            # 检查身体是否近似水平（肩-髋 y 坐标差 < 0.1 归一化）
            try:
                ls = landmarks[11]; rs = landmarks[12]
                lh = landmarks[23]; rh = landmarks[24]
                shoulder_y = (ls.y + rs.y) / 2
                hip_y = (lh.y + rh.y) / 2
                if abs(shoulder_y - hip_y) > 0.15:
                    return False
                # 肘、腕可见性
                if landmarks[13].visibility < 0.3 or landmarks[15].visibility < 0.3:
                    return False
                return True
            except:
                return False

        elif self.exercise_type in ["bicep_curl", "dumbbell_curl"]:
            # 至少一侧手臂可见
            try:
                if landmarks[11].visibility < 0.3 and landmarks[12].visibility < 0.3:
                    return False
                return True
            except:
                return False

        elif self.exercise_type == "running":
            # 下半身关键点可见
            needed = [23,24,25,26,27,28]
            try:
                visible_count = 0
                for idx in needed:
                    if landmarks[idx].visibility > 0.3:
                        visible_count += 1
                return visible_count >= 4
            except:
                return False


        return True

    # ========== 统一调用接口 ==========
    def analyze_exercise(self, landmarks):
        if hasattr(landmarks, 'landmark'):
            landmarks = landmarks.landmark

        res = {
            "count": 0, "score": 0, "feedback": [],
            "angle": 0, "kneeAngle": 0, "hipAngle": 0, "trunkAngle": 0, "stepWidth": 0,
            "pose_class": None,      # 新增：分类结果
            "class_confidence": 0    # 新增：分类置信度
        }

        # 动作预校验（所有运动都做）
        if not self._validate_action_type(landmarks):
            res["feedback"] = [f"未检测到有效的{self.exercise_type}动作，请调整姿势"]
            return res

        # ========== 调用具体运动分析器 ==========
        if self.exercise_type == "running":
            moving = self.motion_detector.update(landmarks)
            r = self._analyze_running(landmarks)
            res.update(r)
            if not moving:
                res["feedback"].append("请开始运动")

        elif self.exercise_type == "pushup":
            r = self.pushup_detector.analyze(landmarks)
            res["count"] = r.get("count", 0)
            res["score"] = r.get("score", 0)
            res["feedback"] = r.get("feedback", [])
            res["angle"] = r.get("angle", 0)
            res["trunkAngle"] = r.get("trunkAngle", 0)

        elif self.exercise_type in ("bicep_curl", "dumbbell_curl"):
            r = self.dumbbell_detector.analyze(landmarks)
            res["count"] = r.get("count", 0)
            res["score"] = r.get("score", 0)
            res["feedback"] = r.get("feedback", [])
            res["angle"] = r.get("angle", 0)

        # ========== 调用 LightGBM 分类器 ==========
        if self.lgb_model is not None:
            exercise_type, confidence = self._classify_pose(landmarks)
            if exercise_type:
                res["pose_class"] = exercise_type
                res["class_confidence"] = round(confidence, 3)
                # 验证当前运动类型是否与分类结果一致
                if exercise_type != self.exercise_type and confidence > 0.7:
                    res["feedback"].append(f"动作标准度极低，建议多加训练和学习")

        if res["count"] > self.counter:
            self.counter = res["count"]
        return res
    
    # ========== 自动检测运动类型 ==========
    def detect_exercise_type(self, landmarks):
        """
        自动检测当前姿态的运动类型
        返回 (运动类型, 置信度)
        """
        if self.lgb_model is None:
            return None, 0.0
        
        return self._classify_pose(landmarks)

    # ========================= 跑步分析（原有逻辑，略作调整） =========================
    def _analyze_running(self, lm):
        a = UserPoseAnalyzer()
        shoulder = a.get_point(lm, 11)
        hip = a.get_point(lm, 23)
        knee = a.get_point(lm, 25)
        ankle = a.get_point(lm, 27)

        if not all([shoulder, hip, knee, ankle]):
            return {'score': 0, 'feedback': ['请全身入镜，特别是腿部'],
                    'kneeAngle': 0, 'hipAngle': 0, 'trunkAngle': 0}

        try:
            a = UserPoseAnalyzer()
            shoulder = a.get_point(lm, 11)
            hip = a.get_point(lm, 23)
            knee = a.get_point(lm, 25)
            ankle = a.get_point(lm, 27)

            if not all([shoulder, hip, knee, ankle]):
                return {'score': 0, 'feedback': ['未检测到全身姿势'], 'kneeAngle': 0, 'hipAngle': 0, 'trunkAngle': 0}

            knee_angle = a.calculate_angle(hip, knee, ankle)
            hip_angle = a.calculate_angle(shoulder, hip, knee)
            trunk_angle = abs(shoulder[1] - hip[1]) * 100
            step = 0.25

            print(f"[跑步分析] 原始角度 - 膝盖: {knee_angle}, 髋部: {hip_angle}, 躯干: {trunk_angle}")

            knee_angle = self.smoother_knee.update(knee_angle)
            hip_angle = self.smoother_hip.update(hip_angle)
            trunk_angle = self.smoother_trunk.update(trunk_angle)

            print(f"[跑步分析] 平滑后角度 - 膝盖: {knee_angle}, 髋部: {hip_angle}, 躯干: {trunk_angle}")

            std = self.standard_data['running']
            print(f"[跑步分析] 标准范围 - 膝盖: {std['knee']}, 髋部: {std['hip']}, 躯干: {std['trunk']}")
            
            score = 0
            fb = []

            if std['knee'][0] <= knee_angle <= std['knee'][1]:
                score += 33
                print("[跑步分析] 膝盖角度在标准范围内")
            else:
                fb.append("膝盖姿势需调整")
                print("[跑步分析] 膝盖角度不在标准范围内")

            if std['hip'][0] <= hip_angle <= std['hip'][1]:
                score += 33
                print("[跑步分析] 髋部角度在标准范围内")
            else:
                fb.append("跨步姿势需调整")
                print("[跑步分析] 髋部角度不在标准范围内")

            if trunk_angle <= std['trunk'][1]:
                score += 34
                print("[跑步分析] 躯干角度在标准范围内")
            else:
                fb.append("躯干前倾过度")
                print("[跑步分析] 躯干角度不在标准范围内")
            
            print(f"[跑步分析] 最终得分: {score}")

            return {
                'score': min(100, max(0, score)),
                'feedback': fb[:2],
                'kneeAngle': round(knee_angle, 1),
                'hipAngle': round(hip_angle, 1),
                'trunkAngle': round(trunk_angle, 1),
                'stepWidth': 0.25
            }
        except Exception as e:
            print(f"跑步分析错误：{e}")
            return {'score': 0, 'feedback': ['检测失败']}

    # ========================= 视频分析（保持原样） =========================
    def analyze_video(self, path):
        self.set_exercise_type(self.exercise_type)
        if self.exercise_type == 'running':
            return self._analyze_running_video(path)
        else:
            return self._analyze_action_video(path)

    def _analyze_running_video(self, path):
        cap = cv2.VideoCapture(path)
        scores = []
        while cap.isOpened():
            ret, f = cap.read()
            if not ret: break
            i, r = self.running_detector.find_pose(f, draw=False)
            if r and r.pose_landmarks:
                res = self.analyze_exercise(r.pose_landmarks[0].landmark)
                scores.append(res['score'])
        cap.release()
        avg = np.mean(scores) if scores else 0
        return f"跑步姿势评分：{avg:.1f}分", None

    def _analyze_action_video(self, path):
        cap = cv2.VideoCapture(path)
        detector = RunningPoseDetector()
        while cap.isOpened():
            ret, f = cap.read()
            if not ret: break
            i, r = detector.find_pose(f)
            if r and r.pose_landmarks:
                self.analyze_exercise(r.pose_landmarks[0].landmark)
        cap.release()
        return f"完成次数：{self.counter}", None


if __name__ == "__main__":
    print("✅ 跑步、俯卧撑、哑铃弯举分析器（已集成 LightGBM 姿态分类）")