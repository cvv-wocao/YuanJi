#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动检测视频中的运动类型 - 使用强规则判断 + 模型辅助
"""

import cv2
import json
import os
import sys
import numpy as np

# 添加项目根目录到路径（使用绝对路径确保可靠性）
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)
print("Added path:", project_root)
from running.ExerciseAnalyzer import ExerciseAnalyzer


def extract_geometric_features(landmarks):
    """提取关键几何特征用于运动判断"""
    try:
        # 获取关键点
        l_shoulder = landmarks[11]
        r_shoulder = landmarks[12]
        l_hip = landmarks[23]
        r_hip = landmarks[24]
        l_knee = landmarks[25]
        r_knee = landmarks[26]
        l_ankle = landmarks[27]
        r_ankle = landmarks[28]
        l_elbow = landmarks[13]
        r_elbow = landmarks[14]
        
        # 计算肩和髋的中心点
        shoulder_y = (l_shoulder.y + r_shoulder.y) / 2
        hip_y = (l_hip.y + r_hip.y) / 2
        shoulder_x = (l_shoulder.x + r_shoulder.x) / 2
        hip_x = (l_hip.x + r_hip.x) / 2
        
        # 肩髋垂直距离 - 判断身体是否水平（俯卧撑特征）
        shoulder_hip_y_diff = abs(shoulder_y - hip_y)
        
        # 计算躯干角度（肩-髋连线与水平线的夹角）
        dx = hip_x - shoulder_x
        dy = hip_y - shoulder_y
        trunk_angle = np.arctan2(dy, abs(dx)) * 180 / np.pi
        
        # 计算膝踝平均高度
        ankle_y = (l_ankle.y + r_ankle.y) / 2
        knee_y = (l_knee.y + r_knee.y) / 2
        
        # 计算身体伸展程度
        body_height = abs(shoulder_y - ankle_y)
        
        # 计算肘部角度（哑铃弯举特征）
        def calculate_angle(a, b, c):
            ba = np.array([a.x - b.x, a.y - b.y, a.z - b.z])
            bc = np.array([c.x - b.x, c.y - b.y, c.z - b.z])
            cosine = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-6)
            angle = np.arccos(np.clip(cosine, -1.0, 1.0))
            return np.degrees(angle)
        
        left_elbow_angle = calculate_angle(l_shoulder, l_elbow, landmarks[15])
        right_elbow_angle = calculate_angle(r_shoulder, r_elbow, landmarks[16])
        avg_elbow_angle = (left_elbow_angle + right_elbow_angle) / 2
        
        # 计算膝盖角度（跑步特征）
        left_knee_angle = calculate_angle(l_hip, l_knee, l_ankle)
        right_knee_angle = calculate_angle(r_hip, r_knee, r_ankle)
        avg_knee_angle = (left_knee_angle + right_knee_angle) / 2
        
        return {
            'shoulder_hip_y_diff': shoulder_hip_y_diff,
            'trunk_angle': trunk_angle,
            'avg_elbow_angle': avg_elbow_angle,
            'avg_knee_angle': avg_knee_angle,
            'body_height': body_height,
            'is_horizontal': shoulder_hip_y_diff < 0.15,  # 身体接近水平
            'is_vertical': trunk_angle > 60,  # 身体接近垂直
        }
    except Exception as e:
        print(f"提取几何特征失败: {e}")
        return None


def classify_by_rules(features, frame_idx):
    """
    基于规则的运动分类
    返回: (运动类型, 置信度, 原因)
    """
    if not features:
        return None, 0, "特征提取失败"
    
    # 俯卧撑判断：身体接近水平
    if features['is_horizontal']:
        return 'pushup', 0.95, f"身体水平(肩髋差={features['shoulder_hip_y_diff']:.3f})"
    
    # 身体垂直时，判断是跑步还是哑铃弯举
    if features['is_vertical']:
        # 哑铃弯举：肘部角度变化大（但单帧看肘部角度范围）
        if features['avg_elbow_angle'] < 120:
            return 'dumbbell_curl', 0.70, f"身体垂直+肘部弯曲({features['avg_elbow_angle']:.1f}°)"
        else:
            return 'running', 0.85, f"身体垂直+肘部伸展({features['avg_elbow_angle']:.1f}°)"
    
    # 默认偏向跑步（因为数据不平衡问题）
    return 'running', 0.60, "默认判断"


def detect_exercise_type(video_path, model_path="pose_classifier.txt", debug=False):
    """自动检测视频中的运动类型"""
    analyzer = ExerciseAnalyzer()
    cap = cv2.VideoCapture(video_path)
    
    frame_count = 0
    rule_results = []
    model_results = []
    all_probabilities = []
    
    print("="*70)
    print("开始分析视频，使用强规则优先 + 模型辅助")
    print("="*70)
    
    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            break
        
        # 每3帧分析一次
        if frame_count % 3 == 0:
            img, results = analyzer.running_detector.find_pose(frame, draw=False)
            if results and results.pose_landmarks:
                lm = results.pose_landmarks[0]
                
                # 1. 先提取几何特征用于规则判断
                geo_features = extract_geometric_features(lm)
                
                # 2. 规则判断
                rule_type, rule_conf, rule_reason = classify_by_rules(geo_features, frame_count)
                if rule_type:
                    rule_results.append(rule_type)
                
                # 3. 模型判断（用于对比）
                model_type = None
                model_conf = 0
                try:
                    features = analyzer._extract_landmark_features(lm)
                    features = features.reshape(1, -1)
                    
                    import pandas as pd
                    feature_names = [f'lm_{i}_{dim}' for i in range(33) for dim in ['x', 'y', 'z', 'visibility']]
                    feature_names += [f'geo_{i}' for i in range(12)]
                    features_df = pd.DataFrame(features, columns=feature_names)
                    
                    pred_proba = analyzer.lgb_model.predict_proba(features_df)[0]
                    class_names = {0: "running", 1: "dumbbell_curl", 2: "pushup"}
                    
                    # 模型预测（记录但不优先使用）
                    class_id = np.argmax(pred_proba)
                    model_conf = float(pred_proba[class_id])
                    model_type = class_names.get(class_id, f"class_{class_id}")
                    model_results.append(model_type)
                    
                    all_probabilities.append({
                        'frame': frame_count,
                        'rule_type': rule_type,
                        'model_type': model_type,
                        'rule_conf': rule_conf,
                        'model_conf': model_conf,
                        'geo': geo_features,
                        'probs': {
                            'running': float(pred_proba[0]),
                            'dumbbell_curl': float(pred_proba[1]),
                            'pushup': float(pred_proba[2])
                        }
                    })
                    
                    if debug:
                        geo_info = ""
                        if geo_features:
                            geo_info = f"水平={geo_features['is_horizontal']} 垂直={geo_features['is_vertical']} 肩髋差={geo_features['shoulder_hip_y_diff']:.3f}"
                        
                        print(f"帧 {frame_count:3d} | 规则: {rule_type:15s}({rule_conf:.2f}) | "
                              f"模型: {model_type:15s}({model_conf:.2f}) | {geo_info} | {rule_reason}")
                
                except Exception as e:
                    if debug:
                        print(f"帧 {frame_count:3d} | 规则: {rule_type:15s} | 模型分析失败: {e}")
        
        frame_count += 1
        if frame_count > 150:
            break
    
    cap.release()
    
    # 汇总分析
    print("\n" + "="*70)
    print("汇总分析:")
    print("="*70)
    
    from collections import Counter
    
    # 规则投票结果
    if rule_results:
        rule_counter = Counter(rule_results)
        print("\n规则判断统计:")
        for t, c in rule_counter.most_common():
            print(f"  {t:15s}: {c:4d} ({c/len(rule_results)*100:5.1f}%)")
    
    # 模型投票结果（仅用于对比）
    if model_results:
        model_counter = Counter(model_results)
        print("\n模型判断统计（仅参考）:")
        for t, c in model_counter.most_common():
            print(f"  {t:15s}: {c:4d} ({c/len(model_results)*100:5.1f}%)")
    
    # 最终决策：完全信任规则判断！
    final_type = None
    if rule_results:
        rule_counter = Counter(rule_results)
        final_type = rule_counter.most_common(1)[0][0]
        print(f"\n🏆 最终决策（基于规则）: {final_type}")
    else:
        final_type = "running"
        print(f"\n🏆 最终决策（默认）: {final_type}")
    
    return final_type


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description="自动检测视频中的运动类型（规则优先）")
    parser.add_argument("video_path", help="视频文件路径")
    parser.add_argument("--model", default="pose_classifier.txt", help="模型文件路径")
    parser.add_argument("--debug", action="store_true", help="启用详细调试输出")
    
    args = parser.parse_args()
    
    if not os.path.exists(args.video_path):
        print(f"错误: 视频文件 {args.video_path} 不存在")
        return
    
    if not os.path.exists(args.model):
        print(f"错误: 模型文件 {args.model} 不存在")
        print("请先运行 train_pose_classifier.py 训练模型")
        return
    
    exercise_type = detect_exercise_type(args.video_path, args.model, args.debug)
    print(f"\n✅ 检测到的运动类型: {exercise_type}")


if __name__ == "__main__":
    main()
