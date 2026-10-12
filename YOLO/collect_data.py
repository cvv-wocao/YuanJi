#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
收集姿态数据用于训练
"""

import cv2
import json
import os
import sys

# 添加项目根目录到路径（使用绝对路径确保可靠性）
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)
from running.ExerciseAnalyzer import ExerciseAnalyzer

def collect_data(exercise_type, video_path, output_file):
    """收集指定运动类型的数据"""
    # 切换到running目录，以便找到MediaPipe模型文件
    original_cwd = os.getcwd()
    running_dir = os.path.join(os.path.dirname(__file__), '..', 'running')
    os.chdir(running_dir)
    
    analyzer = ExerciseAnalyzer()
    os.chdir(original_cwd)  # 切换回原目录
    
    data = []
    
    # 检查是否是目录
    if os.path.isdir(video_path):
        # 遍历目录中的所有视频文件
        for video_file in os.listdir(video_path):
            if video_file.endswith(('.mp4', '.avi', '.mov')):
                video_file_path = os.path.join(video_path, video_file)
                print(f"处理视频: {video_file}")
                cap = cv2.VideoCapture(video_file_path)
                frame_count = 0
                
                while cap.isOpened():
                    ret, frame = cap.read()
                    if not ret:
                        break
                    
                    # 每5帧收集一次数据
                    if frame_count % 5 == 0:
                        img, results = analyzer.running_detector.find_pose(frame, draw=False)
                        if results and results.pose_landmarks:
                            lm = results.pose_landmarks[0]
                            # 提取特征
                            features = analyzer._extract_landmark_features(lm)
                            # 对于跑步，添加角度特征
                            # if exercise_type == "running":
                            #     angle_features = analyzer._extract_running_angles(lm)
                            #     features = np.concatenate([features, angle_features])
                            
                            sample = {
                                'features': features.tolist(),
                                'label': exercise_type
                            }
                            data.append(sample)
                    
                    frame_count += 1
                    if frame_count > 100:  # 最多收集100帧
                        break
                
                cap.release()
    else:
        # 处理单个视频文件
        cap = cv2.VideoCapture(video_path)
        frame_count = 0
        
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            
            # 每5帧收集一次数据
            if frame_count % 5 == 0:
                img, results = analyzer.running_detector.find_pose(frame, draw=False)
                if results and results.pose_landmarks:
                    lm = results.pose_landmarks[0]
                    # 提取特征
                    features = analyzer._extract_landmark_features(lm)
                    # 对于跑步，添加角度特征
                    # if exercise_type == "running":
                    #     angle_features = analyzer._extract_running_angles(lm)
                    #     features = np.concatenate([features, angle_features])
                    
                    sample = {
                        'features': features.tolist(),
                        'label': exercise_type
                    }
                    data.append(sample)
            
            frame_count += 1
            if frame_count > 100:  # 最多收集100帧
                break
        
        cap.release()
    
    # 保存数据
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w') as f:
        json.dump(data, f)
    
    print(f"收集了 {len(data)} 个样本，保存到 {output_file}")

def process_images(exercise_type, image_dir, output_file):
    """处理指定运动类型的图片"""
    # 切换到running目录，以便找到MediaPipe模型文件
    original_cwd = os.getcwd()
    running_dir = os.path.join(os.path.dirname(__file__), '..', 'running')
    os.chdir(running_dir)
    
    analyzer = ExerciseAnalyzer()
    os.chdir(original_cwd)  # 切换回原目录
    
    data = []
    
    # 检查目录是否存在
    if not os.path.exists(image_dir):
        print(f"警告: 目录 {image_dir} 不存在，跳过图片处理")
    else:
        for image_file in os.listdir(image_dir):
            if image_file.endswith(('.jpg', '.jpeg', '.png')):
                image_path = os.path.join(image_dir, image_file)
                frame = cv2.imread(image_path)
                
                if frame is not None:
                    img, results = analyzer.running_detector.find_pose(frame, draw=False)
                    if results and results.pose_landmarks:
                        lm = results.pose_landmarks[0]
                        # 提取特征
                        features = analyzer._extract_landmark_features(lm)
                        # 对于跑步，添加角度特征
                        # if exercise_type == "running":
                        #     angle_features = analyzer._extract_running_angles(lm)
                        #     features = np.concatenate([features, angle_features])
                        
                        sample = {
                            'features': features.tolist(),
                            'label': exercise_type
                        }
                        data.append(sample)
                        print(f"处理图片: {image_file}")
    
    # 保存数据
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w') as f:
        json.dump(data, f)
    
    print(f"处理了 {len(data)} 张图片，保存到 {output_file}")

if __name__ == "__main__":
    # 导入numpy
    import numpy as np
    
    # 收集跑步数据（处理整个目录）
    collect_data("running", "videos/running", "training_data/running_data.json")
    # 收集哑铃弯举数据（处理整个目录）
    collect_data("dumbbell_curl", "videos/dumbbell", "training_data/dumbbell_curl_data.json")
    # 收集俯卧撑数据（处理整个目录）
    collect_data("pushup", "videos/pushup", "training_data/pushup_data.json")
    
    # 处理图片数据
    process_images("running", "training_images/running", "training_data/running_images.json")
    process_images("dumbbell_curl", "training_images/dumbbell", "training_data/dumbbell_curl_images.json")
    process_images("pushup", "training_images/pushup", "training_data/pushup_images.json")
    
    print("数据收集完成")