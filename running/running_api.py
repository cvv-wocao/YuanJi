#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
跑步姿势分析 API 服务
为 yuanjiapp 提供姿势分析的后端接口
"""

import os
import sys
import cv2
import time
import traceback
import numpy as np
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
from RunningPoseModule import RunningPoseDetector
from ExerciseAnalyzer import ExerciseAnalyzer
from PushUpAnalyzer import PushUpAnalyzer
from DumbbellAnalyzer import DumbbellAnalyzer

app = Flask(__name__)
# 配置CORS，允许所有跨域请求
CORS(app, resources={
    r"/api/*": {
        "origins": "*",
        "methods": ["GET", "POST", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

# 全局变量
detector = None
exercise_analyzer = None
pushup_analyzer = None
dumbbell_analyzer = None

def initialize_detector():
    """初始化检测器"""
    global detector, exercise_analyzer, pushup_analyzer, dumbbell_analyzer
    try:
        detector = RunningPoseDetector()
        exercise_analyzer = ExerciseAnalyzer()
        pushup_analyzer = PushUpAnalyzer()
        dumbbell_analyzer = DumbbellAnalyzer()
        print("跑步姿势检测器初始化成功")
        print("运动分析器初始化成功")
        print("俯卧撑高精度分析器初始化成功")
        print("哑铃弯举高精度分析器初始化成功")
    except Exception as e:
        print(f"初始化检测器失败: {e}")
        detector = None
        exercise_analyzer = None
        pushup_analyzer = None
        dumbbell_analyzer = None

# 初始化检测器
initialize_detector()

@app.route('/api/analyze/standard', methods=['GET'])
def analyze_standard():
    """分析标准姿势"""
    try:
        if detector is None:
            return jsonify({"success": False, "message": "检测器未初始化"})
        
        # 这里可以分析标准视频，或者返回默认的标准姿势数据
        standard_data = detector.standard_data
        
        return jsonify({
            "success": True,
            "data": {
                "knee_angle_range": standard_data['knee_angle_range'],
                "hip_angle_range": standard_data['hip_angle_range'],
                "trunk_angle_range": standard_data['trunk_angle_range'],
                "step_width_max": standard_data['step_width_max']
            }
        })
    except Exception as e:
        print(f"分析标准姿势失败: {e}")
        return jsonify({"success": False, "message": str(e)})

@app.route('/api/analyze/video', methods=['POST'])
def analyze_video():
    """分析上传的视频"""
    try:
        if detector is None:
            return jsonify({"success": False, "message": "检测器未初始化"})
        
        # 检查是否有文件上传
        if 'video' not in request.files:
            return jsonify({"success": False, "message": "请上传视频文件"})
        
        video_file = request.files['video']
        if video_file.filename == '':
            return jsonify({"success": False, "message": "未选择文件"})
        
        # 创建临时视频文件夹
        temp_dir = "temp_videos"
        if not os.path.exists(temp_dir):
            os.makedirs(temp_dir)
            print(f"创建临时视频文件夹: {temp_dir}")
        
        # 保存上传的视频
        video_path = os.path.join(temp_dir, f"temp_video_{time.time()}.mp4")
        video_file.save(video_path)
        
        # 分析视频
        print(f"开始分析视频: {video_path}")
        report, output_path = detector.analyze_video(video_path)
        print(f"视频分析完成，报告: {report}")
        print(f"处理后的视频路径: {output_path}")
        
        # 读取分析结果
        # 从 detector 中获取实际的分析结果
        if hasattr(detector, 'user_analyzer') and detector.user_analyzer:
            # 检查 user_analyzer 的属性
            print(f"user_analyzer 属性: {dir(detector.user_analyzer)}")
            print(f"user_analyzer.score: {getattr(detector.user_analyzer, 'score', '不存在')}")
            print(f"user_analyzer.knee_angle: {getattr(detector.user_analyzer, 'knee_angle', '不存在')}")
            print(f"user_analyzer.hip_angle: {getattr(detector.user_analyzer, 'hip_angle', '不存在')}")
            print(f"user_analyzer.trunk_angle: {getattr(detector.user_analyzer, 'trunk_angle', '不存在')}")
            print(f"user_analyzer.step_width: {getattr(detector.user_analyzer, 'step_width', '不存在')}")
            print(f"user_analyzer.feedback: {getattr(detector.user_analyzer, 'feedback', '不存在')}")
            
            # 从 user_analyzer 中获取分析结果
            analysis_result = {
                "score": detector.user_analyzer.score if hasattr(detector.user_analyzer, 'score') else 85,
                "kneeAngle": detector.user_analyzer.knee_angle if hasattr(detector.user_analyzer, 'knee_angle') else 160,
                "hipAngle": detector.user_analyzer.hip_angle if hasattr(detector.user_analyzer, 'hip_angle') else 170,
                "trunkAngle": detector.user_analyzer.trunk_angle if hasattr(detector.user_analyzer, 'trunk_angle') else 8,
                "stepWidth": detector.user_analyzer.step_width if hasattr(detector.user_analyzer, 'step_width') else 10,
                "suggestion": "\n".join(detector.user_analyzer.feedback) if hasattr(detector.user_analyzer, 'feedback') else "姿势良好，继续保持！",
                "processedVideo": output_path if output_path else ""
            }
        else:
            # 如果没有分析结果，使用默认值
            analysis_result = {
                "score": 85,
                "kneeAngle": 160,
                "hipAngle": 170,
                "trunkAngle": 8,
                "stepWidth": 10,
                "suggestion": "姿势良好，继续保持！",
                "processedVideo": output_path if output_path else ""
            }
        
        # 打印最终的分析结果
        print(f"最终分析结果: {analysis_result}")
        
        # 删除临时文件
        if os.path.exists(video_path):
            os.remove(video_path)
        
        return jsonify({"success": True, "data": analysis_result})
    except Exception as e:
        print(f"分析视频失败: {e}")
        return jsonify({"success": False, "message": str(e)})

@app.route('/api/analyze/realtime', methods=['POST'])
def analyze_realtime():
    """实时分析摄像头帧"""
    try:
        if detector is None:
            return jsonify({"success": False, "message": "检测器未初始化"})
        
        # 获取图片数据
        image_data = request.files.get('image')
        if not image_data:
            return jsonify({"success": False, "message": "未提供图片数据"})
        
        # 创建临时图片文件夹
        temp_dir = "temp_images"
        if not os.path.exists(temp_dir):
            os.makedirs(temp_dir)
            print(f"创建临时图片文件夹: {temp_dir}")
        
        # 保存临时图片
        image_path = os.path.join(temp_dir, f"temp_image_{time.time()}.jpg")
        image_data.save(image_path)
        
        # 读取图片
        img = cv2.imread(image_path)
        if img is None:
            return jsonify({"success": False, "message": "无法读取图片"})
        
        # 处理帧
        img, feedback, score = detector.process_frame(img, draw=False)
        
        # 提取分析结果
        # 这里需要从 detector.user_analyzer 中获取最新的分析结果
        analysis_result = {
            "score": score,
            "kneeAngle": 165,
            "hipAngle": 175,
            "trunkAngle": 7,
            "stepWidth": 12,
            "suggestion": "\n".join(feedback) if feedback else "姿势良好，继续保持！"
        }
        
        # 删除临时文件
        if os.path.exists(image_path):
            os.remove(image_path)
        
        return jsonify({"success": True, "data": analysis_result})
    except Exception as e:
        print(f"分析实时帧失败: {e}")
        return jsonify({"success": False, "message": str(e)})

@app.route('/api/video/<filename>', methods=['GET'])
def serve_video(filename):
    """提供处理后的视频文件"""
    try:
        # 首先检查 output_videos 文件夹
        output_dir = "output_videos"
        video_path = os.path.join(os.getcwd(), output_dir, filename)
        
        # 如果在 output_videos 中不存在，检查当前目录
        if not os.path.exists(video_path):
            video_path = os.path.join(os.getcwd(), filename)
        
        if not os.path.exists(video_path):
            return jsonify({"success": False, "message": "视频文件不存在"})
        
        # 获取文件所在目录和文件名
        directory = os.path.dirname(video_path)
        if not directory:
            directory = os.getcwd()
        
        # 返回视频文件
        return send_from_directory(directory, filename, as_attachment=False)
    except Exception as e:
        print(f"提供视频文件失败: {e}")
        return jsonify({"success": False, "message": str(e)})

@app.route('/api/analyze/exercise', methods=['POST'])
def analyze_exercise():
    """分析运动姿势"""
    try:
        # 获取运动类型
        exercise_type = request.form.get('exercise_type', 'running')
        
        # 检查是否有文件上传
        if 'video' not in request.files:
            return jsonify({"success": False, "message": "请上传视频文件"})
        
        video_file = request.files['video']
        if video_file.filename == '':
            return jsonify({"success": False, "message": "未选择文件"})
        
        # 创建临时视频文件夹
        temp_dir = "temp_videos"
        if not os.path.exists(temp_dir):
            os.makedirs(temp_dir)
            print(f"创建临时视频文件夹: {temp_dir}")
        
        # 保存上传的视频
        video_path = os.path.join(temp_dir, f"temp_video_{time.time()}.mp4")
        video_file.save(video_path)
        
        # 分析视频
        print(f"开始分析运动: {exercise_type}, 视频: {video_path}")
        
        if exercise_type == "running":
            # 使用跑步分析器
            if detector is None:
                return jsonify({"success": False, "message": "跑步分析器未初始化"})
            report, output_path = detector.analyze_video(video_path)
            
            # 只返回文件名，不包含路径
            processed_video_filename = os.path.basename(output_path) if output_path else ""
            
            # 读取分析结果
            if hasattr(detector, 'user_analyzer') and detector.user_analyzer:
                analysis_result = {
                    "score": detector.user_analyzer.score if hasattr(detector.user_analyzer, 'score') else 85,
                    "kneeAngle": detector.user_analyzer.knee_angle if hasattr(detector.user_analyzer, 'knee_angle') else 160,
                    "hipAngle": detector.user_analyzer.hip_angle if hasattr(detector.user_analyzer, 'hip_angle') else 170,
                    "trunkAngle": detector.user_analyzer.trunk_angle if hasattr(detector.user_analyzer, 'trunk_angle') else 8,
                    "stepWidth": detector.user_analyzer.step_width if hasattr(detector.user_analyzer, 'step_width') else 10,
                    "suggestion": "\n".join(detector.user_analyzer.feedback) if hasattr(detector.user_analyzer, 'feedback') else "姿势良好，继续保持！",
                    "processedVideo": processed_video_filename
                }
            else:
                analysis_result = {
                    "score": 85,
                    "kneeAngle": 160,
                    "hipAngle": 170,
                    "trunkAngle": 8,
                    "stepWidth": 10,
                    "suggestion": "姿势良好，继续保持！",
                    "processedVideo": processed_video_filename
                }
        elif exercise_type == "pushup":
            # 使用俯卧撑高精度分析器
            if pushup_analyzer is None:
                return jsonify({"success": False, "message": "俯卧撑分析器未初始化"})
            report, output_path = pushup_analyzer.analyze_video(video_path)
            
            # 只返回文件名，不包含路径
            processed_video_filename = os.path.basename(output_path) if output_path else ""
            
            # 构建分析结果
            analysis_result = {
                "score": pushup_analyzer.analyze_pushup.__globals__.get('score', 0),
                "counter": pushup_analyzer.counter,
                "angle": 0,
                "suggestion": report,
                "processedVideo": processed_video_filename,
                "depth": 0,
                "body_alignment": 0
            }
        elif exercise_type == "bicep_curl":
            # 使用哑铃弯举高精度分析器
            if dumbbell_analyzer is None:
                return jsonify({"success": False, "message": "哑铃弯举分析器未初始化"})
            report, output_path = dumbbell_analyzer.analyze_video(video_path)
            
            # 只返回文件名，不包含路径
            processed_video_filename = os.path.basename(output_path) if output_path else ""
            
            # 构建分析结果
            analysis_result = {
                "score": dumbbell_analyzer.analyze_dumbbell_curl.__globals__.get('score', 0),
                "counter": dumbbell_analyzer.counter,
                "angle": 0,
                "suggestion": report,
                "processedVideo": processed_video_filename,
                "stability": 0
            }
        else:
            # 使用运动分析器
            if exercise_analyzer is None:
                return jsonify({"success": False, "message": "运动分析器未初始化"})
            exercise_analyzer.set_exercise_type(exercise_type)
            report, output_path = exercise_analyzer.analyze_video(video_path)
            
            # 只返回文件名，不包含路径
            processed_video_filename = os.path.basename(output_path) if output_path else ""
            
            # 从运动分析器获取实际分析结果
            # 检查运动分析器的属性
            if hasattr(exercise_analyzer, 'last_result') and exercise_analyzer.last_result:
                last_result = exercise_analyzer.last_result
                analysis_result = {
                    "score": last_result.get('score', 0),
                    "counter": last_result.get('counter', 0),
                    "angle": last_result.get('angle', 0),
                    "suggestion": "\n".join(last_result.get('feedback', ["运动分析完成"])),
                    "processedVideo": processed_video_filename
                }
            else:
                # 如果没有分析结果，使用默认值
                analysis_result = {
                    "score": 0,
                    "counter": 0,
                    "angle": 0,
                    "suggestion": "未检测到有效运动",
                    "processedVideo": processed_video_filename
                }
        
        # 打印最终的分析结果
        print(f"最终分析结果: {analysis_result}")
        
        # 删除临时文件
        if os.path.exists(video_path):
            os.remove(video_path)
        
        return jsonify({"success": True, "data": analysis_result})
    except Exception as e:
        print(f"分析运动失败: {e}")
        return jsonify({"success": False, "message": str(e)})

if __name__ == '__main__':
    print("启动跑步姿势分析 API 服务...")
    print("服务地址: http://localhost:5002")
    print("API 接口:")
    print("  GET  /api/analyze/standard - 分析标准姿势")
    print("  POST /api/analyze/video - 分析上传的视频")
    print("  POST /api/analyze/realtime - 实时分析摄像头帧")
    print("  POST /api/analyze/exercise - 分析运动姿势")
    print("  GET  /api/video/<filename> - 获取处理后的视频")
    
    # 使用不同的启动方式，确保服务能够持续运行
    app.run(host='0.0.0.0', port=5002, debug=False, threaded=True, use_reloader=False)
