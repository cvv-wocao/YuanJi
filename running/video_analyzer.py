#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
视频分析脚本
被SpringBoot后端调用，负责分析视频并保存结果到数据库

命令行参数:
--video: 视频文件路径
--username: 用户名（用于关联用户）
--exercise-type: 运动类型（running/pushup/dumbbell_curl）

输出:
JSON格式的分析结果
"""

import os
import sys
import json
import argparse
import cv2
import numpy as np

# 添加当前目录到路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from ExerciseAnalyzer import ExerciseAnalyzer
from database import Database

def analyze_video(video_path, username, exercise_type):
    """分析视频并返回结果"""
    print(f"开始分析视频: {video_path}")
    print(f"用户名: {username}")
    print(f"运动类型: {exercise_type}")

    # 初始化分析器
    analyzer = ExerciseAnalyzer()
    
    # 设置运动类型（直接使用传入的运动类型，不再自动检测）
    analyzer.set_exercise_type(exercise_type)
    print(f"已设置运动类型为: {exercise_type}")

    # 初始化数据库
    db = Database()

    # 获取用户ID（如果用户不存在则自动创建）
    user = db.get_user_by_username(username)
    user_id = user['id']
    print(f"用户ID: {user_id}")

    # 打开视频
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        return json.dumps({
            'success': False,
            'error': f"无法打开视频文件: {video_path}"
        })

    # 获取视频信息
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    print(f"视频信息: FPS={fps}, 总帧数={total_frames}")

    # 分析变量
    scores = []
    knee_angles = []
    hip_angles = []
    trunk_angles = []
    overall_angles = []
    feedback_list = []
    frame_count = 0
    frame_skip = max(1, int(fps // 10))  # 根据帧率动态调整采样间隔
    print(f"采样间隔: 每 {frame_skip} 帧分析一次")

    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break

        frame_count += 1
        if frame_count % frame_skip != 0:
            continue

        try:
            # 检测姿态
            img, results = analyzer.running_detector.find_pose(frame, draw=False)

            if results and results.pose_landmarks:
                lm = results.pose_landmarks[0]

                # 分析运动（使用传入的运动类型）
                res = analyzer.analyze_exercise(lm)

                score = res.get('score', 0)
                knee_angle = res.get('kneeAngle', 0)
                hip_angle = res.get('hipAngle', 0)
                trunk_angle = res.get('trunkAngle', 0)
                overall_angle = res.get('angle', 0)
                feedback = res.get('feedback', [])

                if score > 0:
                    scores.append(score)
                if knee_angle > 0:
                    knee_angles.append(knee_angle)
                if hip_angle > 0:
                    hip_angles.append(hip_angle)
                if trunk_angle > 0:
                    trunk_angles.append(trunk_angle)
                if overall_angle > 0:
                    overall_angles.append(overall_angle)
                if feedback:
                    feedback_list.extend(feedback)

        except Exception as e:
            print(f"分析帧失败: {e}")
            continue

    cap.release()

    # 计算平均值
    avg_score = round(np.mean(scores), 1) if scores else 0
    avg_knee_angle = round(np.mean(knee_angles), 1) if knee_angles else 0
    avg_hip_angle = round(np.mean(hip_angles), 1) if hip_angles else 0
    avg_trunk_angle = round(np.mean(trunk_angles), 1) if trunk_angles else 0
    avg_overall_angle = round(np.mean(overall_angles), 1) if overall_angles else 0

    # 去重反馈
    feedback_list = list(set(feedback_list))[:3] if feedback_list else ["未检测到有效动作"]

    print(f"分析完成 - 有效帧数: {len(scores)}, 平均得分: {avg_score}")
    print(f"反馈列表: {feedback_list}")

    # 保存到数据库
    try:
        record_id = db.save_exercise_record(
            user_id,
            exercise_type,
            duration=None,
            calories=None,
            video_url=video_path
        )

        report_id = db.save_analysis_report(
            record_id,
            avg_score,
            len(scores),
            feedback_list,
            knee_angle=avg_knee_angle,
            hip_angle=avg_hip_angle,
            trunk_angle=avg_trunk_angle,
            overall_angle=avg_overall_angle,
            ai_analysis=None
        )

        print(f"保存成功: record_id={record_id}, report_id={report_id}")

    except Exception as e:
        print(f"保存到数据库失败: {e}")
        return json.dumps({
            'success': False,
            'error': f"保存到数据库失败: {str(e)}"
        })

    # 构建结果
    result = {
        'success': True,
        'username': username,
        'user_id': user_id,
        'exercise_type': exercise_type,
        'video_name': os.path.basename(video_path),
        'avg_score': avg_score,
        'analyzed_frames': len(scores),
        'total_frames': total_frames,
        'feedback': feedback_list,
        'avg_knee_angle': avg_knee_angle,
        'avg_hip_angle': avg_hip_angle,
        'avg_trunk_angle': avg_trunk_angle,
        'avg_overall_angle': avg_overall_angle,
        'record_id': record_id,
        'report_id': report_id,
        'message': '分析完成'
    }

    return json.dumps(result, ensure_ascii=False)

def main():
    parser = argparse.ArgumentParser(description='视频运动分析脚本')
    parser.add_argument('--video', required=True, help='视频文件路径')
    parser.add_argument('--username', required=True, help='用户名')
    parser.add_argument('--exercise-type', required=True, choices=['running', 'dumbbell_curl', 'pushup'], 
                        help='运动类型')

    args = parser.parse_args()

    # 检查视频文件是否存在
    if not os.path.exists(args.video):
        print(json.dumps({
            'success': False,
            'error': f"视频文件不存在: {args.video}"
        }))
        sys.exit(1)

    # 检查运动类型是否有效
    valid_types = ['running', 'dumbbell_curl', 'pushup']
    if args.exercise_type not in valid_types:
        print(json.dumps({
            'success': False,
            'error': f"无效的运动类型: {args.exercise_type}，有效值: {valid_types}"
        }))
        sys.exit(1)

    # 执行分析
    result = analyze_video(args.video, args.username, args.exercise_type)
    print(result)

if __name__ == '__main__':
    main()