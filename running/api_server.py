import time
import traceback
import _thread as thread

from flask import Flask, request, jsonify, make_response
from flask_cors import CORS
from ExerciseAnalyzer import ExerciseAnalyzer
import cv2
import numpy as np
from collections import deque
from database import Database
from motion_detector import MotionDetector
from jwt_utils import get_username_from_token, get_user_id_from_token

# AI 实时反馈相关变量（在 api_server.py 中独立定义，避免导入问题）
MAX_BUFFER_FRAMES = 10          # 缓存最近 10 帧
ai_frame_interval = 3           # 每 3 秒调用一次 AI
last_ai_frame_time = 0          # 上次调用 AI 的时间戳（秒）
ai_feedback_cache = "检测中..."  # 缓存的 AI 反馈文本
frame_data_buffer = []          # 缓存最近几帧的数据用于 AI 分析

def update_ai_feedback(exercise_type, frame_buffer):
    """在线程中调用 AI 生成实时反馈"""
    global ai_feedback_cache
    try:
        if not frame_buffer:
            return
        
        # 计算统计数据
        avg_score = sum(f.get('score', 0) for f in frame_buffer) / len(frame_buffer)
        avg_knee = sum(f.get('knee_angle', 0) for f in frame_buffer) / len(frame_buffer)
        avg_hip = sum(f.get('hip_angle', 0) for f in frame_buffer) / len(frame_buffer)
        
        # 收集所有反馈
        all_feedback = []
        for f in frame_buffer:
            all_feedback.extend(f.get('feedback', []))
        
        # 去重并限制数量
        unique_feedback = list(set(all_feedback))[:3]
        
        # 生成 AI 反馈文本
        if unique_feedback:
            ai_feedback_cache = "、".join(unique_feedback)
        else:
            ai_feedback_cache = "姿势良好，继续保持！"
            
    except Exception as e:
        print(f"AI 反馈更新失败: {e}")
        ai_feedback_cache = "分析中..."
app = Flask(__name__)
# 使用 Flask-CORS 扩展配置 CORS
CORS(app, resources={r"/api/*": {"origins": "http://localhost:5173", "supports_credentials": True}})

analyzer = ExerciseAnalyzer()
db = Database()

@app.route('/api/analyze/frame', methods=['POST'])
def analyze_frame():
    try:
        global last_ai_frame_time, ai_feedback_cache, frame_data_buffer
        exercise = request.form.get("exercise_type", "running")
        
        # 检查是否有图片文件
        if 'image' not in request.files:
            return jsonify({"success": False, "msg": "没有收到图片文件"})
        
        image_file = request.files['image']
        
        # 读取图片数据
        image_bytes = image_file.read()
        nparr = np.frombuffer(image_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if frame is None:
            return jsonify({"success": False, "msg": "无法解码图片"})
        
        # 设置运动类型
        analyzer.set_exercise_type(exercise)
        
        # 使用 MediaPipe 检测姿态
        img, results = analyzer.running_detector.find_pose(frame, draw=False)
        
        if not results or not results.pose_landmarks:
            return jsonify({
                "success": True,
                "count": 0,
                "score": 0,
                "feedback": ["未检测到人体"],
                "angle": 0,
                "kneeAngle": 0,
                "hipAngle": 0,
                "trunkAngle": 0,
                "skeleton": []
            })
        
        # 获取关键点
        lm = results.pose_landmarks[0]
        
        # 分析运动
        res = analyzer.analyze_exercise(lm)
        
        # 提取骨骼关键点坐标（归一化到 0-1 范围）
        skeleton = []
        h, w = frame.shape[:2]
        for landmark in results.pose_landmarks[0]:
            skeleton.append([landmark.x, landmark.y])

        # ---- 缓存帧数据用于 AI 实时反馈 ----
        frame_info = {
            'score': res.get('score', 0),
            'count': res.get('counter', 0),
            'knee_angle': res.get('kneeAngle', 0),
            'hip_angle': res.get('hipAngle', 0),
            'trunk_angle': res.get('trunkAngle', 0),
            'feedback': res.get('feedback', [])
        }
        frame_data_buffer.append(frame_info)
        if len(frame_data_buffer) > MAX_BUFFER_FRAMES:
            frame_data_buffer.pop(0)

        # 定时调用 AI 更新实时反馈
        current_time = time.time()
        if current_time - last_ai_frame_time >= ai_frame_interval:
            last_ai_frame_time = current_time
            thread.start_new_thread(update_ai_feedback, (exercise, frame_data_buffer.copy()))

        return jsonify({ 
            "success": True, 
            "count": res.get("counter", 0), 
            "score": res.get("score", 0), 
            "feedback": res.get("feedback", []),
            "ai_realtime_feedback": ai_feedback_cache,  # 添加这一行
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
    try:
        exercise = request.form.get("exercise_type")
        print("\n========================")
        print("📡 /api/analyze/video 被调用")
        if exercise:
            print(f"📥 收到运动类型: {exercise}")
        else:
            print("📥 未收到运动类型，将自动检测")

        video = request.files["video"]
        save_path = "temp_video.mp4"
        video.save(save_path)
        print(f"💾 视频已保存: {save_path}")

        # 检查模型是否加载
        if analyzer.lgb_model is None:
            print("⚠️ 警告: LightGBM 模型未加载，无法进行运动类型检测")
        else:
            print(f"✅ LightGBM 模型已加载: {type(analyzer.lgb_model)}")

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
            
            print(f"警告：检测到的运动类型({detected_name})与选择的模块({selected_name})不匹配")
            
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

        # 运动检测器（每个视频独立）
        from motion_detector import MotionDetector
        motion_detector = MotionDetector(threshold=0.0005, min_motion_frames=1)

        scores = []
        total_count = 0
        feedback_list = []
        frame_count = 0
        motion_frame_count = 0

        # 保存角度值（仅运动帧）
        knee_angles = []
        hip_angles = []
        trunk_angles = []
        overall_angles = []

        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break

            frame_count += 1
            try:
                # 统一姿态检测
                img, results = analyzer.running_detector.find_pose(frame, draw=False)

                if results and results.pose_landmarks:
                    lm = results.pose_landmarks[0]

                    # 运动检测
                    moving = motion_detector.update(lm)
                    if not moving:
                        print(f"第{frame_count}帧静止，跳过")
                        continue

                    motion_frame_count += 1
                    # 分析运动（此处 analyze_exercise 内部可能还会检测运动，但无大碍）
                    res = analyzer.analyze_exercise(lm)

                    score = res.get("score", 0)
                    count = res.get("counter", 0)  # 注意字段名可能是 count 或 counter
                    fb = res.get("feedback", [])

                    knee_angle = res.get("kneeAngle", 0)
                    hip_angle = res.get("hipAngle", 0)
                    trunk_angle = res.get("trunkAngle", 0)
                    overall_angle = res.get("angle", 0)

                    print(f"第{frame_count}帧(运动) | 得分:{score} | 计数:{count} | 反馈:{fb}")
                    print(f"   角度: 膝盖={knee_angle}, 髋部={hip_angle}, 躯干={trunk_angle}, 动作={overall_angle}")

                    if score > 0:
                        scores.append(score)
                    if count > 0:
                        total_count = count
                    if fb:
                        feedback_list.extend(fb)
                    if knee_angle > 0:
                        knee_angles.append(knee_angle)
                    if hip_angle > 0:
                        hip_angles.append(hip_angle)
                    if trunk_angle > 0:
                        trunk_angles.append(trunk_angle)
                    if overall_angle > 0:
                        overall_angles.append(overall_angle)
                else:
                    print(f"第{frame_count}帧: 未检测到人体")

            except Exception as e:
                print(f"第{frame_count}帧 分析失败: {str(e)}")
                continue

        cap.release()

        print("\n========================")
        print(f"总帧数: {frame_count} | 运动帧数: {motion_frame_count}")

        # 有效运动帧占比过低则判定无效
        if motion_frame_count < frame_count * 0.05:
            return jsonify({
                "success": False,
                "msg": "视频中有效运动帧过少（占比<5%），请确保动作清晰、全身入镜",
                "score": 0, "count": 0, "feedback": ["无效视频"]
            })

        print(f"所有得分: {scores}")
        avg_score = round(np.mean(scores), 1) if scores else 0
        print(f"最终平均分: {avg_score}")
        print(f"最终计数: {total_count}")
        print(f"最终反馈: {feedback_list}")

        avg_knee_angle = round(np.mean(knee_angles), 1) if knee_angles else 0
        avg_hip_angle = round(np.mean(hip_angles), 1) if hip_angles else 0
        avg_trunk_angle = round(np.mean(trunk_angles), 1) if trunk_angles else 0
        avg_overall_angle = round(np.mean(overall_angles), 1) if overall_angles else 0

        print(f"平均角度: 膝盖={avg_knee_angle}, 髋部={avg_hip_angle}, 躯干={avg_trunk_angle}, 动作={avg_overall_angle}")

        feedback = list(set(feedback_list))[:3]
        if not feedback:
            feedback = ["未检测到有效动作"]

        # 保存到数据库
        try:
            # 获取用户信息
            username = get_username_from_token()
            user_id = get_user_id_from_token()
            print(f"用户信息 - username: {username}, user_id: {user_id}")
            
            record_id = db.save_exercise_record(
                user_id,
                exercise,
                duration=None,
                calories=None,
                video_url="temp_video.mp4"
            )
            report_id = db.save_analysis_report(
                record_id,
                avg_score,
                total_count,
                feedback,
                knee_angle=avg_knee_angle,
                hip_angle=avg_hip_angle,
                trunk_angle=avg_trunk_angle,
                overall_angle=avg_overall_angle
            )
            suggestions = generate_suggestions(exercise, avg_score, feedback)
            for suggestion in suggestions:
                db.save_suggestion(
                    report_id,
                    suggestion['type'],
                    suggestion['content'],
                    suggestion['priority']
                )
            print(f"分析结果已保存到数据库，记录ID: {record_id}, 报告ID: {report_id}")
        except Exception as db_error:
            print(f"数据库保存失败: {db_error}")

        return jsonify({
            "success": True,
            "score": avg_score,
            "count": total_count,
            "feedback": feedback,
            "kneeAngle": avg_knee_angle,
            "hipAngle": avg_hip_angle,
            "trunkAngle": avg_trunk_angle,
            "angle": avg_overall_angle
        })

    except Exception as e:
        print(f"\n全局崩溃: {str(e)}")
        traceback.print_exc()
        return jsonify({
            "success": False, "score": 0, "count": 0, "feedback": [f"错误:{str(e)}"]
        })

def detect_video_exercise_type(video_path):
    """
    自动检测视频中的运动类型
    """
    cap = cv2.VideoCapture(video_path)
    exercise_counts = {}
    frame_count = 0
    sample_interval = 10  # 每10帧采样一次
    
    print(f"\n🔍 开始检测视频运动类型: {video_path}")
    
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
                    # 检测运动类型
                    exercise_type, confidence = analyzer.detect_exercise_type(lm)
                    print(f"  帧 {frame_count}: 检测结果={exercise_type}, 置信度={confidence:.4f}")
                    if exercise_type and confidence > 0.4:
                        if exercise_type not in exercise_counts:
                            exercise_counts[exercise_type] = 0
                        exercise_counts[exercise_type] += 1
                        print(f"    ✓ 计入统计: {exercise_type}")
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
    
    print(f"\n📊 检测统计结果:")
    print(f"  总分析帧数: {frame_count}")
    print(f"  各类型计数: {exercise_counts}")
    
    # 投票决定运动类型
    if exercise_counts:
        detected_type = max(exercise_counts, key=exercise_counts.get)
        max_count = exercise_counts[detected_type]
        total_count = sum(exercise_counts.values())
        ratio = max_count / total_count if total_count > 0 else 0
        print(f"  最终检测结果: {detected_type} (占比: {ratio:.2%})")
        return detected_type
    else:
        print(f"  未检测到有效运动类型，默认返回: running")
        return "running"  # 默认返回跑步

def generate_suggestions(exercise_type, score, feedback):
    """根据运动类型、得分和反馈生成运动建议"""
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

# 添加获取历史记录的 API 接口（支持用户ID）
@app.route('/api/records', methods=['GET'])
def get_records():
    """获取运动历史记录"""
    try:
        user_id = request.args.get('user_id', 1)
        limit = int(request.args.get('limit', 10))
        records = db.get_recent_records(user_id, limit)
        return jsonify({
            'success': True,
            'records': records
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'msg': str(e)
        })

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

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5002, debug=True)