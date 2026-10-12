#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
详细分析运动分类问题
"""

import cv2
import numpy as np
import os
import sys
import json
from collections import Counter

# 添加项目根目录
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)
from running.ExerciseAnalyzer import ExerciseAnalyzer


def analyze_single_frame(landmarks, analyzer):
    """分析单帧的详细分类信息"""
    try:
        features = analyzer._extract_landmark_features(landmarks)
        features = features.reshape(1, -1)
        
        import pandas as pd
        feature_names = [f'lm_{i}_{dim}' for i in range(33) for dim in ['x', 'y', 'z', 'visibility']]
        feature_names += [f'geo_{i}' for i in range(12)]
        features_df = pd.DataFrame(features, columns=feature_names)
        
        pred_proba = analyzer.lgb_model.predict_proba(features_df)[0]
        class_names = {0: "running", 1: "dumbbell_curl", 2: "pushup"}
        
        return {
            'probabilities': {
                class_names[i]: float(pred_proba[i]) 
                for i in range(3)
            },
            'predicted': class_names[np.argmax(pred_proba)],
            'confidence': float(np.max(pred_proba))
        }
    except Exception as e:
        print(f"分析失败: {e}")
        return None


def analyze_video_detailed(video_path, analyzer):
    """详细分析视频的分类情况"""
    if not os.path.exists(video_path):
        print(f"错误: 视频文件 {video_path} 不存在")
        return
    
    print(f"正在分析视频: {video_path}")
    
    cap = cv2.VideoCapture(video_path)
    frame_count = 0
    all_predictions = []
    detailed_results = []
    
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        
        # 每5帧分析一次
        if frame_count % 5 == 0:
            img, results = analyzer.running_detector.find_pose(frame, draw=False)
            if results and results.pose_landmarks:
                lm = results.pose_landmarks[0]
                result = analyze_single_frame(lm, analyzer)
                if result:
                    result['frame'] = frame_count
                    all_predictions.append(result['predicted'])
                    detailed_results.append(result)
                    
                    print(f"帧 {frame_count:4d}: {result['predicted']:15s} "
                          f"置信度: {result['confidence']:.3f} | "
                          f"概率: running={result['probabilities']['running']:.3f}, "
                          f"dumbbell={result['probabilities']['dumbbell_curl']:.3f}, "
                          f"pushup={result['probabilities']['pushup']:.3f}")
        
        frame_count += 1
        if frame_count > 200:
            break
    
    cap.release()
    
    # 统计结果
    if all_predictions:
        print("\n" + "="*60)
        print("分类统计结果:")
        print("="*60)
        
        counter = Counter(all_predictions)
        total = len(all_predictions)
        
        for label, count in counter.most_common():
            percentage = (count / total) * 100
            print(f"  {label:15s}: {count:4d} ({percentage:5.1f}%)")
        
        print(f"\n  总分析帧数: {total}")
        
        # 计算平均概率
        if detailed_results:
            avg_probs = {}
            for cls in ['running', 'dumbbell_curl', 'pushup']:
                avg_probs[cls] = np.mean([r['probabilities'][cls] for r in detailed_results])
            
            print("\n平均概率分布:")
            for cls, prob in avg_probs.items():
                print(f"  {cls:15s}: {prob:.3f}")
    
    return detailed_results


def main():
    parser = None
    try:
        import argparse
        parser = argparse.ArgumentParser(description="详细分析运动分类")
        parser.add_argument("video_path", help="视频文件路径")
        args = parser.parse_args()
        video_path = args.video_path
    except:
        # 默认使用temp_video.mp4
        video_path = "temp_video.mp4"
        if not os.path.exists(video_path):
            print(f"默认视频 {video_path} 不存在")
            print("请提供视频路径作为参数")
            return
    
    print("加载分析器...")
    analyzer = ExerciseAnalyzer()
    
    if analyzer.lgb_model is None:
        print("模型加载失败！")
        return
    
    analyze_video_detailed(video_path, analyzer)


if __name__ == "__main__":
    main()
