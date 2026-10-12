import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from flask import Flask, request, jsonify, make_response
from flask_cors import CORS
from ExerciseAnalyzer import ExerciseAnalyzer
from werkzeug.exceptions import ClientDisconnected
import cv2
import numpy as np
from database import Database
import _thread as thread
import base64
import hashlib
import hmac
import json
import ssl
from collections import Counter, defaultdict
import time
from datetime import datetime
from time import mktime
#from motion_detector import MotionDetector
from urllib.parse import urlencode
from urllib.parse import urlparse
from wsgiref.handlers import format_date_time
import websocket
import traceback
last_landmarks = {}
ai_feedback_cache = "检测中..."   # 原来是 ["检测中..."]
app = Flask(__name__)
# 增加请求大小限制，100MB
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024 * 1024
# 使用 Flask-CORS 扩展配置 CORS
CORS(app)
analyzer = ExerciseAnalyzer()
db = Database()

# 导入 JWT 工具（用于解析 Spring Boot JWT token）
try:
    from jwt_utils import get_user_id_from_token
    print("✅ JWT 工具模块加载成功")
except ImportError:
    print("❌ JWT 工具模块导入失败，使用默认实现")
    
    def get_user_id_from_token():
        """从请求头中获取token并验证，返回用户ID；无效或缺失返回默认用户ID 1"""
        auth_header = request.headers.get('Authorization')
        if not auth_header:
            print("未提供 Authorization 头，使用默认用户ID 1")
            return 1
        try:
            token = auth_header.split(' ')[1] if len(auth_header.split(' ')) > 1 else auth_header
            print(f"收到的token: {token}")
            
            # 尝试从token中提取用户ID
            if '-' in token:
                user_id_str = token.split('-')[0]
                if user_id_str.isdigit():
                    user_id = int(user_id_str)
                    print(f"从token中提取用户ID: {user_id}")
                    return user_id
            # 尝试直接使用token作为用户ID
            if token.isdigit():
                user_id = int(token)
                print(f"直接使用token作为用户ID: {user_id}")
                return user_id
        except Exception as e:
            print(f"Token 解析失败: {e}")
        
        print("无效 Token，使用默认用户ID 1")
        return 1

# ============ 星火 AI 客户端类 ============
class SparkClient:
    def __init__(self, appid, api_key, api_secret, spark_url, domain):
        self.appid = appid
        self.api_key = api_key
        self.api_secret = api_secret
        self.spark_url = spark_url
        self.domain = domain
        self.answer = ""
        self.is_first_content = False
        self.thinking_content = ""

    class WsParam:
        def __init__(self, appid, api_key, api_secret, spark_url):
            self.appid = appid
            self.api_key = api_key
            self.api_secret = api_secret
            self.host = urlparse(spark_url).netloc
            self.path = urlparse(spark_url).path
            self.spark_url = spark_url

        def create_url(self):
            now = datetime.now()
            date = format_date_time(mktime(now.timetuple()))
            signature_origin = f"host: {self.host}\ndate: {date}\nGET {self.path} HTTP/1.1"
            signature_sha = hmac.new(self.api_secret.encode('utf-8'),
                                     signature_origin.encode('utf-8'),
                                     digestmod=hashlib.sha256).digest()
            signature_sha_base64 = base64.b64encode(signature_sha).decode('utf-8')
            authorization_origin = f'api_key="{self.api_key}", algorithm="hmac-sha256", headers="host date request-line", signature="{signature_sha_base64}"'
            authorization = base64.b64encode(authorization_origin.encode('utf-8')).decode('utf-8')
            v = {"authorization": authorization, "date": date, "host": self.host}
            return self.spark_url + '?' + urlencode(v)

    def _on_message(self, ws, message):
        try:
            data = json.loads(message)
            code = data['header']['code']

            if code != 0:
                print(f"星火 API 错误: {code}, {data}")
                ws.close()
                return

            choices = data["payload"]["choices"]
            status = choices["status"]
            text = choices['text'][0]

            # 收集思维链内容（可选）
            if 'reasoning_content' in text and text['reasoning_content']:
                self.thinking_content += text["reasoning_content"]
                self.is_first_content = True

            # 收集最终回答
            if 'content' in text and text['content']:
                if self.is_first_content:
                    # 思维链结束，开始正式回答
                    self.is_first_content = False
                self.answer += text["content"]

            if status == 2:
                ws.close()
        except Exception as e:
            print(f"处理消息时出错: {e}")
            ws.close()

    def _on_error(self, ws, error):
        print(f"WebSocket 错误: {error}")

    def _on_close(self, ws, one, two):
        pass

    def _on_open(self, ws):
        def run(*args):
            try:
                # 构建请求参数
                request_data = {
                    "header": {
                        "app_id": self.appid,
                        "uid": "exercise_analyzer"
                    },
                    "parameter": {
                        "chat": {
                            "domain": self.domain,
                            "temperature": 0.7,
                            "max_tokens": 2048,
                            "top_k": 5
                        }
                    },
                    "payload": {
                        "message": {
                            "text": [
                                {
                                    "role": "user",
                                    "content": ws.question
                                }
                            ]
                        }
                    }
                }

                # 如果是 X1 模型，可以添加 thinking 控制
                #if self.domain == "x1":
                    #request_data["parameter"]["chat"]["thinking"] = {"type": "auto"}

                data = json.dumps(request_data)
                ws.send(data)
            except Exception as e:
                print(f"发送请求时出错: {e}")

        thread.start_new_thread(run, ())

    def ask(self, question, timeout=30):
        """
        发送问题并返回完整回答
        :param question: 问题文本
        :param timeout: 超时时间（秒）
        :return: AI 回答文本
        """
        try:
            self.answer = ""
            self.thinking_content = ""
            self.is_first_content = False

            ws_param = self.WsParam(self.appid, self.api_key, self.api_secret, self.spark_url)
            ws_url = ws_param.create_url()

            ws = websocket.WebSocketApp(ws_url,
                                       on_message=self._on_message,
                                       on_error=self._on_error,
                                       on_close=self._on_close,
                                       on_open=self._on_open)
            ws.question = question

            # 运行 WebSocket 连接
            ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

            # 返回最终答案
            return self.answer.strip() if self.answer.strip() else "AI 分析暂时无法获取，请稍后重试。"

        except Exception as e:
            print(f"API 调用失败: {e}")
            traceback.print_exc()
            return f"AI 分析服务暂时不可用: {str(e)}"

# 初始化星火客户端
spark_client = SparkClient(
    appid="5407bd2e",
    api_key="7c5d5cb7470d5897a6379096a41ece49",
    api_secret="ZjJlYmYwZTg1NGRmNmRhMWZlNDBjNThk",
    spark_url="wss://spark-api.xf-yun.com/v1/x1",  # X1 模型地址
    domain="x1"  # 使用 x1 模型
)

# ============ 实时分析 AI 反馈缓存 ============
last_ai_frame_time = 0          # 上次调用 AI 的时间戳（秒）
ai_feedback_cache = "检测中..."  # 缓存的 AI 反馈文本
ai_frame_interval = 5.0         # 每 5 秒调用一次 AI
frame_data_buffer = []           # 缓存最近几帧的数据用于 AI 分析
MAX_BUFFER_FRAMES = 10           # 最多缓存 10 帧数据

def generate_realtime_prompt(exercise_type, recent_data):
    if not recent_data:
        return None

    avg_score = sum(d['score'] for d in recent_data) / len(recent_data)
    last_count = recent_data[-1].get('count', 0)

    if exercise_type == 'running':
        avg_knee = sum(d.get('knee_angle', 0) for d in recent_data) / len(recent_data)
        avg_hip = sum(d.get('hip_angle', 0) for d in recent_data) / len(recent_data)
        avg_trunk = sum(d.get('trunk_angle', 0) for d in recent_data) / len(recent_data)
        metrics = f"""
平均膝盖角度：{avg_knee:.1f}°
平均髋部角度：{avg_hip:.1f}°
平均躯干角度：{avg_trunk:.1f}°"""
        example = """- "膝盖角度保持得很好，继续！"
- "躯干稍微前倾，请挺直背部。"
- "步频可以再快一点，加油！"
- "髋部发力不足，试着向后坐一点。" """
    elif exercise_type in ('dumbbell_curl', 'bicep_curl'):
        avg_elbow = sum(d.get('angle', 0) for d in recent_data) / len(recent_data)
        metrics = f"""
平均肘关节角度：{avg_elbow:.1f}°"""
        example = """- "弯举角度标准，非常好！"
- "下放速度再慢一点，感受肌肉发力。"
- "大臂保持固定，不要晃动。"
- "手腕保持中立，不要弯曲。" """
    elif exercise_type == 'pushup':
        avg_elbow = sum(d.get('angle', 0) for d in recent_data) / len(recent_data)
        metrics = f"""
平均肘关节角度：{avg_elbow:.1f}°"""
        example = """- "身体保持一条直线，核心收紧。"
- "下压时胸部贴近地面，效果更好。"
- "肘部不要过度外展，保持45度角。"
- "上升时不要锁死肘关节。" """
    else:
        metrics = ""
        example = """- "动作标准，继续保持！"
- "注意控制节奏，不要过快。" """

    prompt = f"""你是一个专业的运动教练。根据用户最近几秒的运动数据，生成一句**简短、实时、鼓励性**的反馈（不超过25个字），指出优点或改进点。

运动类型：{exercise_type}
平均得分：{avg_score:.1f}/100
当前完成次数：{last_count}
{metrics}

重要要求：
- 即使平均得分很低（例如低于30分），也请给出**积极、具体、可操作**的改进建议，例如“膝盖弯曲角度再大一点”“肘部不要完全伸直”等，避免只说“加油”“努力”等空话。
- 反馈要简洁、直接、有指导性。

示例输出：
{example}
请直接输出反馈句子，不要有多余解释。"""
    return prompt

# ============ 运动类型检测函数 ============
def extract_geometric_features(lm):
    """提取关键几何特征用于运动判断"""
    try:
        l_shoulder = lm[11]
        r_shoulder = lm[12]
        l_hip = lm[23]
        r_hip = lm[24]
        l_knee = lm[25]
        r_knee = lm[26]
        l_ankle = lm[27]
        r_ankle = lm[28]
        l_elbow = lm[13]
        r_elbow = lm[14]

        shoulder_y = (l_shoulder.y + r_shoulder.y) / 2
        hip_y = (l_hip.y + r_hip.y) / 2
        shoulder_x = (l_shoulder.x + r_shoulder.x) / 2
        hip_x = (l_hip.x + r_hip.x) / 2

        shoulder_hip_y_diff = abs(shoulder_y - hip_y)

        dx = hip_x - shoulder_x
        dy = hip_y - shoulder_y
        trunk_angle = np.arctan2(dy, abs(dx)) * 180 / np.pi

        ankle_y = (l_ankle.y + r_ankle.y) / 2
        knee_y = (l_knee.y + r_knee.y) / 2

        def calculate_angle(a, b, c):
            ba = np.array([a.x - b.x, a.y - b.y, a.z - b.z])
            bc = np.array([c.x - b.x, c.y - b.y, c.z - b.z])
            cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-6)
            angle = np.arccos(np.clip(cosine, -1.0, 1.0))
            return np.degrees(angle)

        left_elbow_angle = calculate_angle(l_shoulder, l_elbow, lm[15])
        right_elbow_angle = calculate_angle(r_shoulder, r_elbow, lm[16])
        avg_elbow_angle = (left_elbow_angle + right_elbow_angle) / 2

        left_knee_angle = calculate_angle(l_hip, l_knee, l_ankle)
        right_knee_angle = calculate_angle(r_hip, r_knee, r_ankle)
        avg_knee_angle = (left_knee_angle + right_knee_angle) / 2

        return {
            'shoulder_hip_y_diff': shoulder_hip_y_diff,
            'trunk_angle': trunk_angle,
            'avg_elbow_angle': avg_elbow_angle,
            'avg_knee_angle': avg_knee_angle,
            'body_height': abs(shoulder_y - ankle_y),
            'is_horizontal': shoulder_hip_y_diff < 0.15,
            'is_vertical': trunk_angle > 60,
        }
    except Exception as e:
        print(f"  [几何特征提取失败] {e}")
        return None


def classify_by_rules(features):
    """
    基于几何规则的运动分类
    返回: (运动类型, 置信度, 判断依据)
    """
    if not features:
        return None, 0, "特征提取失败"

    if features['is_horizontal']:
        return 'pushup', 0.95, f"身体水平(肩髋差={features['shoulder_hip_y_diff']:.3f})"

    if features['is_vertical']:
        if features['avg_elbow_angle'] < 120:
            return 'dumbbell_curl', 0.75, f"身体垂直+肘部弯曲({features['avg_elbow_angle']:.1f}°)"
        else:
            return 'running', 0.85, f"身体垂直+肘部伸展({features['avg_elbow_angle']:.1f}°)"

    return 'running', 0.60, "默认判断"


def detect_video_exercise_type(video_path):
    """
    自动检测视频中的运动类型（增强版：机器学习 + 几何规则双重判断）
    采用纯加权相加方式：ML权重0.4，规则权重0.6
    """
    cap = cv2.VideoCapture(video_path)
    exercise_counts = {}
    frame_count = 0
    sample_interval = 10  # 每10帧采样一次

    # ========== 用于多帧投票的列表 ==========
    frame_results = []  # 存储ML模型的分类结果
    rule_results = []  # 存储规则判断的结果

    # ========== 加权置信度累加器（ML权重0.4，规则权重0.6） ==========
    weighted_scores = {
        'running': 0.0,
        'dumbbell_curl': 0.0,
        'pushup': 0.0
    }

    ML_WEIGHT = 0.5   # ML模型权重
    RULE_WEIGHT = 0.5  # 几何规则权重

    print(f"\n🔍 开始检测视频运动类型: {video_path}")
    print("="*70)
    print("采用双重判断机制：机器学习模型 + 几何规则判断")
    print(f"权重分配：ML模型={ML_WEIGHT}，几何规则={RULE_WEIGHT}")
    print("="*70)

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        frame_count += 1
        if frame_count % sample_interval == 0:
            try:
                # 检测姿态
                img, results = analyzer.running_detector.find_pose(frame, draw=False)
                if results and results.pose_landmarks:
                    lm = results.pose_landmarks[0]

                    # ========== 1. 机器学习模型判断 ==========
                    shoulder_width = abs(lm[11].x - lm[12].x)
                    is_side_view = shoulder_width < 0.1  # 侧视阈值

                    exercise_type, confidence = analyzer.detect_exercise_type(lm)
                    print(
                        f"  帧 {frame_count}: ML模型={exercise_type}, 置信度={confidence:.4f}")

                    if is_side_view:
                        confidence *= 0.6  # 降低侧视角的可信度
                        print(f"    [侧视角调整后] 置信度={confidence:.4f}")

                    # ========== 2. 几何规则判断 ==========
                    geo_features = extract_geometric_features(lm)
                    rule_type, rule_conf, rule_reason = classify_by_rules(geo_features)
                    print(f"    几何规则={rule_type}, 置信度={rule_conf:.4f}, 依据: {rule_reason}")

                    # ========== 3. 纯加权相加投票：ML权重0.4，规则权重0.6 ==========

                    # ML模型加权得分 (权重0.4)
                    if exercise_type and confidence > 0.0:
                        weighted_scores[exercise_type] += confidence * ML_WEIGHT
                        frame_results.append(exercise_type)

                    # 几何规则加权得分 (权重0.6)
                    if rule_type:
                        weighted_scores[rule_type] += rule_conf * RULE_WEIGHT
                        rule_results.append(rule_type)

                    # 打印当前帧的加权得分情况
                    print(f"    → 加权得分: running={weighted_scores['running']:.4f}, "
                          f"dumbbell_curl={weighted_scores['dumbbell_curl']:.4f}, "
                          f"pushup={weighted_scores['pushup']:.4f}")

                else:
                    print(f"  帧 {frame_count}: 未检测到人体")
            except Exception as e:
                print(f"  帧 {frame_count}: 检测出错 - {str(e)}")
                continue

        # 最多分析300帧
        if frame_count > 300:
            print(f"  已达到最大分析帧数(300)，停止检测")
            break

    cap.release()

    print(f"\n{'='*70}")
    print("📊 检测统计结果")
    print('='*70)
    print(f"  总分析帧数: {frame_count}")
    print(f"  ML模型投票: {dict(Counter(frame_results))}")
    print(f"  几何规则投票: {dict(Counter(rule_results))}")

    # ========== 加权得分最终决策 ==========
    print(f"\n📈 最终加权得分:")
    print(f"  running:       {weighted_scores['running']:.4f}")
    print(f"  dumbbell_curl: {weighted_scores['dumbbell_curl']:.4f}")
    print(f"  pushup:        {weighted_scores['pushup']:.4f}")

    # 选择加权得分最高的运动类型
    detected_type = max(weighted_scores, key=weighted_scores.get)
    max_score = weighted_scores[detected_type]

    if max_score > 0:
        print(f"\n🏆 最终决策: {detected_type} (加权得分: {max_score:.4f})")
    else:
        detected_type = "running"
        print(f"\n🏆 最终决策（默认）: {detected_type}")

    print('='*70)
    return detected_type

# ============ 辅助函数 ============
def generate_ai_prompt(exercise_type, avg_score, feedback_list, angles_data):
    """生成给星火 AI 的提示词"""
    feedback_text = "、".join(feedback_list) if feedback_list else "无明显问题"

    prompt = f"""你是一个专业的运动教练，请根据以下运动数据分析用户的表现，给出简短评价（150字以内）和2-3条具体改进建议：

运动类型：{exercise_type}
平均得分：{avg_score}/100
主要问题：{feedback_text}
关键角度（度）：
- 膝盖角度：{angles_data.get('knee', 0)}
- 髋部角度：{angles_data.get('hip', 0)}  
- 躯干角度：{angles_data.get('trunk', 0)}
- 整体角度：{angles_data.get('overall', 0)}

请按以下格式回复：
评价：[简短评价]
建议：
1. [这样运动短期内会对身体产生什么疾病？]
2. [这样运动长期内会对身体产生什么疾病？]
3. [具体调整建议]
4. [具体建议]（如有需要）"""

    return prompt

def generate_suggestions(exercise_type, score, feedback):
    """根据运动类型、得分和反馈生成运动建议（备用方案）"""
    suggestions = []

    # 基础建议
    if score < 60:
        suggestions.append({
            'type': '基础',
            'content': '建议加强基础训练，提高动作规范性',
            'priority': 3
        })
    elif score < 80:
        suggestions.append({
            'type': '基础',
            'content': '动作基本规范，建议加强细节调整',
            'priority': 2
        })
    else:
        suggestions.append({
            'type': '基础',
            'content': '动作规范，继续保持良好状态',
            'priority': 1
        })

    # 运动类型特定建议
    if exercise_type == 'running':
        if score < 70:
            suggestions.append({
                'type': '跑步',
                'content': '注意步幅不宜过大，保持膝盖微屈以减少关节压力',
                'priority': 2
            })
        suggestions.append({
            'type': '跑步',
            'content': '保持核心稳定，避免躯干过度晃动',
            'priority': 2
        })

    elif exercise_type == 'dumbbell_curl':
        if score < 70:
            suggestions.append({
                'type': '哑铃弯举',
                'content': '保持大臂固定，仅依靠小臂力量进行弯举',
                'priority': 2
            })
        suggestions.append({
            'type': '哑铃弯举',
            'content': '控制动作速度，避免借助惯性完成动作',
            'priority': 2
        })

    elif exercise_type == 'pushup':
        if score < 70:
            suggestions.append({
                'type': '俯卧撑',
                'content': '保持身体成一条直线，避免臀部上抬或下沉',
                'priority': 2
            })
        suggestions.append({
            'type': '俯卧撑',
            'content': '控制下降速度，胸部几乎触地后再推起',
            'priority': 2
        })

    # 根据反馈生成建议
    for fb in feedback:
        if '姿势' in fb:
            suggestions.append({
                'type': '姿势',
                'content': '注意保持正确的动作姿势，避免受伤',
                'priority': 3
            })
        elif '节奏' in fb:
            suggestions.append({
                'type': '节奏',
                'content': '保持均匀的运动节奏，提高运动效果',
                'priority': 2
            })

    # 通用建议
    suggestions.append({
        'type': '呼吸',
        'content': '保持均匀呼吸，避免屏息训练',
        'priority': 1
    })

    suggestions.append({
        'type': '频率',
        'content': '建议每周进行3-4次训练，每次30-60分钟',
        'priority': 1
    })

    # 去重并限制数量
    seen = set()
    unique_suggestions = []
    for s in suggestions:
        key = f"{s['type']}_{s['content']}"
        if key not in seen:
            seen.add(key)
            unique_suggestions.append(s)

    return unique_suggestions[:5]  # 最多返回5条建议


# api_server_X1.py 中添加以下路由和辅助函数

import json
from collections import Counter, defaultdict


def aggregate_habit_data(records):
    """聚合原始记录，生成统计数据"""
    if not records:
        return None

    total_count = len(records)
    exercise_types = [r['exercise_type'] for r in records]
    most_common_exercise = Counter(exercise_types).most_common(1)[0][0]

    # 各类型平均得分
    exercise_scores = defaultdict(list)
    for r in records:
        exercise_scores[r['exercise_type']].append(r['score'])
    avg_scores = {ex: round(sum(scores) / len(scores), 1) for ex, scores in exercise_scores.items()}

    # 整体平均分
    overall_avg_score = round(sum(r['score'] for r in records) / total_count, 1)

    # 收集所有反馈，提取关键词（简单统计词频）
    all_feedback = []
    for r in records:
        all_feedback.extend(r['feedback'])
    feedback_counter = Counter(all_feedback)
    top_feedback = [{"text": k, "count": v} for k, v in feedback_counter.most_common(5)]

    # 角度偏差：计算每个运动类型的平均角度（如果有）
    angle_summary = {}
    for ex in set(exercise_types):
        ex_records = [r for r in records if r['exercise_type'] == ex]
        if ex == 'running':
            knee_angles = [r['knee_angle'] for r in ex_records if r['knee_angle']]
            hip_angles = [r['hip_angle'] for r in ex_records if r['hip_angle']]
            trunk_angles = [r['trunk_angle'] for r in ex_records if r['trunk_angle']]
            angle_summary[ex] = {
                'avg_knee': round(sum(knee_angles) / len(knee_angles), 1) if knee_angles else None,
                'avg_hip': round(sum(hip_angles) / len(hip_angles), 1) if hip_angles else None,
                'avg_trunk': round(sum(trunk_angles) / len(trunk_angles), 1) if trunk_angles else None,
            }
        else:  # dumbbell_curl, pushup
            angles = [r['overall_angle'] for r in ex_records if r['overall_angle']]
            angle_summary[ex] = {
                'avg_angle': round(sum(angles) / len(angles), 1) if angles else None
            }

    return {
        'total_records': total_count,
        'most_common_exercise': most_common_exercise,
        'avg_scores_by_type': avg_scores,
        'overall_avg_score': overall_avg_score,
        'top_feedback': top_feedback,
        'angle_summary': angle_summary,
        'records': records  # 可选，用于深度分析
    }


def generate_habit_prompt(habit_data):
    """生成发给星火AI的提示词，用于习惯分析"""
    prompt = f"""你是一个专业的运动健康顾问。请根据以下用户的运动历史数据，分析他的运动习惯和主要不足，并给出3-5条具体、可执行的改进建议。

【总体统计】
- 总运动次数：{habit_data['total_records']} 次
- 最常进行的运动：{habit_data['most_common_exercise']}
- 整体平均得分：{habit_data['overall_avg_score']}/100

【各运动类型平均得分】
{', '.join([f"{k}: {v}分" for k, v in habit_data['avg_scores_by_type'].items()])}

【常见扣分问题】（出现频率较高的反馈）
{', '.join([f"{fb['text']}({fb['count']}次)" for fb in habit_data['top_feedback']]) if habit_data['top_feedback'] else '无明显高频问题'}

【角度数据总结】（标准范围：跑步膝关节约140-160°，髋关节约160-180°，躯干约0-10°前倾；弯举/俯卧撑肘关节约70-90°）
{_format_angle_summary(habit_data['angle_summary'])}

请按以下格式回答：
1. 总体评价：（一句话概括用户运动水平）
2. 主要不足：（列出2-3个最突出的问题）
3. 改进建议：
   - 建议一：...
   - 建议二：...
   - 建议三：...
4. 长期健康提示：（根据不足可能导致的健康风险，以及预防措施）

回答要专业、鼓励性、具体，避免空泛。"""
    return prompt


def _format_angle_summary(angle_summary):
    lines = []
    for ex, angles in angle_summary.items():
        if ex == 'running':
            lines.append(
                f"跑步：膝关节平均{angles.get('avg_knee', '?')}°，髋关节{angles.get('avg_hip', '?')}°，躯干{angles.get('avg_trunk', '?')}°")
        else:
            lines.append(f"{ex}：平均动作角度{angles.get('avg_angle', '?')}°")
    return '\n'.join(lines) if lines else "无角度数据"


@app.route('/api/analysis/habit', methods=['GET'])
def habit_analysis():
    """运动习惯分析接口"""
    try:
        # 添加详细调试日志
        print("\n" + "="*50)
        print("=== AI运动顾问接口请求 ===")
        print(f"请求时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"请求方法: {request.method}")
        print(f"请求路径: {request.path}")
        print(f"请求参数: {dict(request.args)}")
        print(f"Authorization header: {request.headers.get('Authorization')}")
        print(f"请求来源: {request.headers.get('Referer', '未知')}")
        print(f"用户代理: {request.headers.get('User-Agent', '未知')}")
        print(f"客户端IP: {request.remote_addr}")
        
        # 优先从请求参数获取用户名
        username = request.args.get('username')
        print(f"获取到的用户名参数: {username if username else '未提供'}")
        
        if username:
            print(f"→ 使用用户名查询: {username}")
            records = db.get_all_records_for_analysis_by_username(username)
            print(f"→ 查询到记录数: {len(records)}")
            
            if not records:
                print(f"✗ 用户 {username} 没有记录")
                return jsonify({
                    'success': False,
                    'msg': f'用户 {username} 暂无足够的历史记录进行分析（至少需要1条记录）'
                })
            
            habit_data = aggregate_habit_data(records)
            if not habit_data:
                print("✗ 数据聚合失败")
                return jsonify({'success': False, 'msg': '数据聚合失败'})
            
            prompt = generate_habit_prompt(habit_data)
            ai_suggestion = spark_client.ask(prompt)
            
            if not ai_suggestion or "暂时无法获取" in ai_suggestion:
                ai_suggestion = _generate_fallback_suggestion(habit_data)
            
            print("✓ 分析完成，返回结果")
            return jsonify({
                'success': True,
                'username': username,
                'habit_data': habit_data,
                'ai_suggestion': ai_suggestion
            })
        
        # 原有逻辑（保持兼容）
        print("→ 未提供用户名参数，尝试从token获取用户ID")
        user_id = get_user_id_from_token()
        print(f"→ 获取到的用户ID: {user_id}")
        
        records = db.get_all_records_for_analysis(user_id)
        print(f"→ 查询到记录数: {len(records)}")
        
        if not records:
            print(f"✗ 用户ID {user_id} 没有记录")
            return jsonify({
                'success': False,
                'msg': '暂无足够的历史记录进行分析（至少需要1条记录）'
            })
        
        habit_data = aggregate_habit_data(records)
        if not habit_data:
            print("✗ 数据聚合失败")
            return jsonify({'success': False, 'msg': '数据聚合失败'})
        
        prompt = generate_habit_prompt(habit_data)
        ai_suggestion = spark_client.ask(prompt)
        
        if not ai_suggestion or "暂时无法获取" in ai_suggestion:
            ai_suggestion = _generate_fallback_suggestion(habit_data)
        
        print("✓ 分析完成，返回结果")
        return jsonify({
            'success': True,
            'user_id': user_id,
            'habit_data': habit_data,
            'ai_suggestion': ai_suggestion
        })
    except Exception as e:
        print(f"✗ 习惯分析失败: {e}")
        traceback.print_exc()
        return jsonify({'success': False, 'msg': str(e)})


def _generate_fallback_suggestion(habit_data):
    """备用规则建议（当AI不可用时）"""
    suggestions = []
    overall_score = habit_data['overall_avg_score']
    if overall_score < 60:
        suggestions.append("您的整体动作得分偏低，建议从基础动作开始，先保证姿势正确性。")
    elif overall_score < 80:
        suggestions.append("动作基本规范，可以多关注细节调整，如关节角度和发力点。")
    else:
        suggestions.append("动作水平优秀，继续保持，可以尝试增加训练强度。")

    # 根据常见反馈添加建议
    for fb in habit_data['top_feedback'][:2]:
        if '膝盖' in fb['text']:
            suggestions.append("跑步时注意膝盖微屈，落地时避免锁死。")
        elif '肘' in fb['text']:
            suggestions.append("弯举或俯卧撑时，肘部不要过度外展，保持45度角。")
        elif '躯干' in fb['text']:
            suggestions.append("保持躯干挺直，核心收紧，避免弯腰驼背。")

    return "\n".join(suggestions) if suggestions else "继续坚持运动，定期查看分析报告改进姿势。"


# ============ API 路由 ============
@app.route('/api/analyze/frame', methods=['POST'])
def analyze_frame():
    try:
        global last_ai_frame_time, ai_feedback_cache, frame_data_buffer, last_landmarks

        exercise = request.form.get("exercise_type", "running")

        if 'image' not in request.files:
            return jsonify({"success": False, "msg": "没有收到图片文件"})

        image_file = request.files['image']
        image_bytes = image_file.read()
        nparr = np.frombuffer(image_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if frame is None:
            return jsonify({"success": False, "msg": "无法解码图片"})

        analyzer.set_exercise_type(exercise)
        img, results = analyzer.running_detector.find_pose(frame, draw=False)

        if not results or not results.pose_landmarks:
            return jsonify({
                "success": True, "count": 0, "score": 0, "feedback": ["未检测到人体"],
                "angle": 0, "kneeAngle": 0, "hipAngle": 0, "trunkAngle": 0, "skeleton": []
            })

        lm = results.pose_landmarks[0]

        # ========== 运动检测（基于帧间位移）==========
        client_ip = request.remote_addr
        prev_lm = last_landmarks.get(client_ip)
        moving = False
        if prev_lm is not None:
            key_indices = [11, 12, 23, 24, 25, 26, 27, 28]  # 肩、髋、膝、踝
            total_displacement = 0.0
            count = 0
            for idx in key_indices:
                if idx < len(lm) and idx < len(prev_lm):
                    dx = lm[idx].x - prev_lm[idx].x
                    dy = lm[idx].y - prev_lm[idx].y
                    total_displacement += (dx*dx + dy*dy) ** 0.5
                    count += 1
            if count > 0:
                moving = (total_displacement / count) > 0.012   # 阈值可调
        # 更新存储的上一帧
        last_landmarks[client_ip] = lm

        # 分析运动（始终计算，但只有运动帧才用于AI缓存）
        res = analyzer.analyze_exercise(lm)

        # 提取骨骼关键点坐标
        skeleton = []
        for landmark in results.pose_landmarks[0]:
            skeleton.append([landmark.x, landmark.y])
        print(f"后端返回 skeleton 长度: {len(skeleton)}")  # 调试用

        # ========== 只要检测到人体就缓存并定时触发AI ==========
        if results and results.pose_landmarks:
            frame_info = {
                'score': res.get('score', 0),
                'count': res.get('count', 0) or res.get('counter', 0),
                'knee_angle': res.get('kneeAngle', 0),
                'hip_angle': res.get('hipAngle', 0),
                'trunk_angle': res.get('trunkAngle', 0),
                'angle': res.get('angle', 0),
                'feedback': res.get('feedback', [])
            }
            
            print(f"[帧分析] 缓存帧数据: {frame_info}")
            frame_data_buffer.append(frame_info)
            if len(frame_data_buffer) > MAX_BUFFER_FRAMES:
                frame_data_buffer.pop(0)
            print(f"[帧分析] 缓冲区大小: {len(frame_data_buffer)}")

            current_time = time.time()
            print(f"[帧分析] 当前时间: {current_time}, 上次 AI 时间: {last_ai_frame_time}, 间隔: {current_time - last_ai_frame_time}")
            if current_time - last_ai_frame_time >= ai_frame_interval:
                print(f"[帧分析] 调用 AI 更新反馈")
                last_ai_frame_time = current_time
                thread.start_new_thread(update_ai_feedback, (exercise, frame_data_buffer.copy()))
            else:
                print(f"[帧分析] 等待 AI 调用，还需: {ai_frame_interval - (current_time - last_ai_frame_time):.2f} 秒")
        else:
            if not frame_data_buffer:
                ai_feedback_cache = "未检测到人体，请全身入镜"

        # ✅ 添加调试打印：输出即将返回的关键角度值
        print(f"📐 返回角度: angle={res.get('angle', 0)}, knee={res.get('kneeAngle', 0)}")

        return jsonify({
            "success": True,
            "count": res.get("count", 0) or res.get("counter", 0),
            "score": res.get("score", 0),
            "feedback": res.get("feedback", []),
            "ai_realtime_feedback": ai_feedback_cache,
            "angle": res.get("angle", 0),
            "kneeAngle": res.get("kneeAngle", 0),
            "hipAngle": res.get("hipAngle", 0),
            "trunkAngle": res.get("trunkAngle", 0),
            "skeleton": skeleton
        })
    except Exception as e:
        print(f"分析帧时出错: {str(e)}")
        traceback.print_exc()
        return jsonify({"success": False, "msg": str(e)})

@app.route('/api/analyze/standard', methods=['GET'])
def analyze_standard():
    try:
        return jsonify({"success": True, "message": "标准动作分析完成"})
    except Exception as e:
        return jsonify({"success": False, "msg": str(e)})

@app.route('/api/analyze/video', methods=['POST'])
def analyze_video():
    # 1. 首先验证用户身份
    user_id = get_user_id_from_token()
    if user_id is None:
        return jsonify({
            "success": False,
            "msg": "未登录或 token 无效，请重新登录"
        }), 401

    # 初始化变量
    avg_score = 0
    total_count = 0
    feedback = []
    avg_knee_angle = 0
    avg_hip_angle = 0
    avg_trunk_angle = 0
    avg_overall_angle = 0
    ai_analysis = ""
    scores = []
    knee_angles = []
    hip_angles = []
    trunk_angles = []
    overall_angles = []
    frame_count = 0

    try:
        exercise = request.form.get("exercise_type")
        print(f"\n========================")
        if exercise:
            print(f"📥 收到运动类型: {exercise}")
        else:
            print("📥 未收到运动类型，将自动检测")

        video = request.files["video"]
        save_path = "temp_video.mp4"
        video.save(save_path)

        # 只调用一次检测函数，避免结果不一致
        print("🔍 开始检测视频运动类型...")
        detected_exercise = detect_video_exercise_type(save_path)
        print(f"✅ 检测到视频中的运动类型: {detected_exercise}")
        
        # 如果没有提供运动类型，使用检测结果
        if not exercise:
            exercise = detected_exercise
            print(f"🤖 自动设置运动类型为: {exercise}")
        
        # 映射运动类型名称（统一命名）
        exercise_map = {
            "running": "跑步",
            "dumbbell_curl": "哑铃弯举",
            "bicep_curl": "哑铃弯举",
            "pushup": "俯卧撑"
        }
        
        # 如果检测到的运动类型与选择的模块不匹配
        if detected_exercise != exercise and detected_exercise in exercise_map:
            detected_name = exercise_map.get(detected_exercise, detected_exercise)
            selected_name = exercise_map.get(exercise, exercise)
            
            print(f"⚠️ 警告：检测到的运动类型({detected_name})与选择的模块({selected_name})不匹配")
            
            return jsonify({
                "success": False,
                "msg": f"检测到该视频是{detected_name}动作，与您当前选择的{selected_name}模块不匹配，请切换到{detected_name}模块进行分析",
                "detected_exercise": detected_exercise,
                "detected_name": detected_name,
                "selected_exercise": exercise,
                "selected_name": selected_name,
                "score": 0, 
                "count": 0, 
                "feedback": [f"视频类型不匹配：检测到{detected_name}动作"]
            })

        analyzer.set_exercise_type(exercise)
        cap = cv2.VideoCapture(save_path)

        # 跳过帧数
        frame_skip = 3
        frame_idx = 0

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            frame_idx += 1
            frame_count += 1
            if frame_idx % frame_skip != 0:
                continue

            # 缩放帧
            h, w = frame.shape[:2]
            if w > 640:
                scale = 640 / w
                new_w = 640
                new_h = int(h * scale)
                frame = cv2.resize(frame, (new_w, new_h))

            try:
                img, results = analyzer.running_detector.find_pose(frame, draw=False)

                if results and results.pose_landmarks:
                    lm = results.pose_landmarks[0]

                    # 直接分析，不进行外部运动检测
                    res = analyzer.analyze_exercise(lm)

                    score = res.get("score", 0)
                    count = res.get("counter", 0)
                    fb = res.get("feedback", [])

                    knee_angle = res.get("kneeAngle", 0)
                    hip_angle = res.get("hipAngle", 0)
                    trunk_angle = res.get("trunkAngle", 0)
                    overall_angle = res.get("angle", 0)

                    print(f"✅ 第{frame_count}帧 | 得分:{score} | 计数:{count} | 反馈:{fb}")
                    print(f"   角度: 膝盖={knee_angle}, 髋部={hip_angle}, 躯干={trunk_angle}, 动作={overall_angle}")

                    if score > 0:
                        scores.append(score)
                    if count > 0:
                        total_count = count  # 取最后一次出现的计数（也可改为 max）
                    if fb:
                        feedback.extend(fb)
                    if knee_angle > 0:
                        knee_angles.append(knee_angle)
                    if hip_angle > 0:
                        hip_angles.append(hip_angle)
                    if trunk_angle > 0:
                        trunk_angles.append(trunk_angle)
                    if overall_angle > 0:
                        overall_angles.append(overall_angle)
                else:
                    print(f"❌ 第{frame_count}帧: 未检测到人体")

            except Exception as e:
                print(f"💥 第{frame_count}帧 分析失败: {str(e)}")
                continue

        cap.release()

        print(f"\n========================\n📊 总帧数: {frame_count}")

        # 计算平均值
        avg_score = round(np.mean(scores), 1) if scores else 0
        avg_knee_angle = round(np.mean(knee_angles), 1) if knee_angles else 0
        avg_hip_angle = round(np.mean(hip_angles), 1) if hip_angles else 0
        avg_trunk_angle = round(np.mean(trunk_angles), 1) if trunk_angles else 0
        avg_overall_angle = round(np.mean(overall_angles), 1) if overall_angles else 0

        print(f"📊 最终平均分: {avg_score}")
        print(f"📊 最终计数: {total_count}")
        print(f"📊 平均角度: 膝盖={avg_knee_angle}, 髋部={avg_hip_angle}, 躯干={avg_trunk_angle}, 动作={avg_overall_angle}")

        # 处理反馈列表
        feedback = list(set(feedback))[:3] if feedback else ["未检测到有效动作"]

        # 调用星火 AI 分析
        try:
            angles_data = {
                'knee': avg_knee_angle,
                'hip': avg_hip_angle,
                'trunk': avg_trunk_angle,
                'overall': avg_overall_angle
            }
            prompt = generate_ai_prompt(exercise, avg_score, feedback, angles_data)
            print(f"🤖 发送给AI的提示词:\n{prompt}")
            ai_analysis = spark_client.ask(prompt)
            print(f"✨ AI 分析结果:\n{ai_analysis}")
        except Exception as ai_error:
            print(f"⚠️ AI 分析失败: {ai_error}")
            ai_analysis = "AI 分析暂时不可用，请参考基础建议。"
            traceback.print_exc()

        # 获取用户ID（在主函数中获取，避免在异步函数中无法访问request对象）
        user_id = get_user_id_from_token()
        print(f"User ID for analysis: {user_id}")


        # 异步保存数据库
        def save_to_db():
            try:
                # 确保用户存在（如果 users 表中没有该 ID，则自动创建）
                db.ensure_user_exists(user_id)

                record_id = db.save_exercise_record(
                    user_id,
                    exercise,
                    duration=None,
                    calories=None,
                    video_url="temp_video.mp4"
                )
                print(f"✅ [save_to_db] save_exercise_record 成功，record_id={record_id}")

                report_id = db.save_analysis_report(
                    record_id,
                    avg_score,
                    total_count,
                    feedback,
                    knee_angle=avg_knee_angle,
                    hip_angle=avg_hip_angle,
                    trunk_angle=avg_trunk_angle,
                    overall_angle=avg_overall_angle,
                    ai_analysis=ai_analysis
                )
                print(f"✅ [save_to_db] save_analysis_report 成功，report_id={report_id}")

                suggestions = generate_suggestions(exercise, avg_score, feedback)
                for suggestion in suggestions:
                    db.save_suggestion(
                        report_id,
                        suggestion['type'],
                        suggestion['content'],
                        suggestion['priority']
                    )
                print(f"🎉 分析结果已完整保存到数据库，记录ID: {record_id}, 报告ID: {report_id}")
            except Exception as db_error:
                print(f"❌ 数据库保存失败: {db_error}")
                traceback.print_exc()
                # 异步保存数据库

        import _thread
        print("🚀 启动后台线程保存数据库...")
        _thread.start_new_thread(save_to_db, ())

        return jsonify({
            "success": True,
            "score": avg_score,
            "count": total_count,
            "feedback": feedback,
            "kneeAngle": avg_knee_angle,
            "hipAngle": avg_hip_angle,
            "trunkAngle": avg_trunk_angle,
            "angle": avg_overall_angle,
            "ai_analysis": ai_analysis,
            "analysis_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        })

    except ClientDisconnected:
        print("客户端断开连接")
        return jsonify({
            "success": False,
            "msg": "连接中断，请重试",
            "score": 0, "count": 0, "feedback": ["上传中断，请重试"]
        })
    except Exception as e:
        print(f"\n💥 全局崩溃: {str(e)}")
        traceback.print_exc()
        return jsonify({
            "success": False,
            "score": 0,
            "count": 0,
            "feedback": [f"错误:{str(e)}"],
            "ai_analysis": "分析失败，请重试。"
        })

# 添加获取历史记录的 API 接口
@app.route('/api/records', methods=['GET'])
def get_records():
    try:
        # 优先从请求参数获取用户名
        username = request.args.get('username')
        
        if username:
            # 直接按用户名查询记录
            limit = int(request.args.get('limit', 10))
            records = db.get_recent_records_by_username(username, limit)
            print(f"📋 通过用户名 '{username}' 查询到 {len(records)} 条记录")
            return jsonify({'success': True, 'records': records, 'username': username})
        
        # 原有逻辑（保持兼容）
        user_id = get_user_id_from_token()
        limit = int(request.args.get('limit', 10))
        records = db.get_recent_records(user_id, limit)
        print(f"📋 用户 {user_id} 查询到 {len(records)} 条记录")
        return jsonify({'success': True, 'records': records, 'user_id': user_id})
    except Exception as e:
        print(f"获取历史记录失败: {e}")
        return jsonify({'success': False, 'msg': str(e)})


# 添加获取详细报告的 API 接口
@app.route('/api/reports/<int:report_id>', methods=['GET'])
def get_report(report_id):
    """获取详细分析报告"""
    try:
        report = db.get_report_by_id(report_id)
        if report:
            return jsonify({
                'success': True,
                'report': report
            })
        else:
            return jsonify({
                'success': False,
                'msg': '报告不存在'
            })
    except Exception as e:
        return jsonify({
            'success': False,
            'msg': str(e)
        })

# 添加单独测试 AI 分析的接口
@app.route('/api/test/ai', methods=['POST'])
def test_ai_analysis():
    """测试 AI 分析功能"""
    try:
        data = request.get_json()
        exercise = data.get("exercise_type", "running")
        score = data.get("score", 75)
        feedback = data.get("feedback", [])
        angles = data.get("angles", {})

        angles_data = {
            'knee': angles.get('knee', 0),
            'hip': angles.get('hip', 0),
            'trunk': angles.get('trunk', 0),
            'overall': angles.get('overall', 0)
        }

        prompt = generate_ai_prompt(exercise, score, feedback, angles_data)
        ai_analysis = spark_client.ask(prompt)

        return jsonify({
            'success': True,
            'ai_analysis': ai_analysis,
            'prompt': prompt
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'msg': str(e)
        })


def update_ai_feedback(exercise_type, frame_buffer):
    """在后台线程中调用星火大模型，更新 ai_feedback_cache"""
    global ai_feedback_cache
    try:
        if not frame_buffer:
            return

        prompt = generate_realtime_prompt(exercise_type, frame_buffer)
        if not prompt:
            return
        # 调用星火客户端（注意：SparkClient.ask 是同步阻塞的，但在新线程中没问题）
        ai_response = spark_client.ask(prompt)

        # ✅ 添加日志：打印 API 返回的原始内容
        print(f"🤖 实时AI反馈原文: {ai_response}")

        if ai_response and len(ai_response) > 0:
            # 限制长度，确保适合实时显示
            ai_feedback_cache = ai_response.strip()[:50]
        else:
            ai_feedback_cache = "继续加油！"
    except Exception as e:
        print(f"❌ 实时 AI 反馈生成失败: {e}")
        ai_feedback_cache = "姿势检测中..."

if __name__ == '__main__':
    print("🚀 启动运动分析服务器...")
    print("📡 API 地址: http://localhost:5002")
    print("🤖  AI 分析已集成")
    app.run(host="0.0.0.0", port=5002, debug=True, threaded=True, use_reloader=False)