#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
训练LightGBM姿态分类模型
"""

import numpy as np
import pandas as pd
import lightgbm as lgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report
import json
import os

def load_training_data(data_file):
    with open(data_file, 'r') as f:
        data = json.load(f)
    # ========== 新增验证 ==========
    lengths = [len(item['features']) for item in data]
    unique_lengths = set(lengths)
    print(f"特征长度分布: {unique_lengths}")
    if len(unique_lengths) != 1 or 144 not in unique_lengths:
        raise ValueError(f"特征维度错误，期望全部为144，实际分布 {unique_lengths}")
    # =============================
    
    X = []
    y = []
    label_map = {'running':0, 'dumbbell_curl':1, 'pushup':2}
    
    for idx, item in enumerate(data):
        features = item['features']
        # 严格校验：特征长度必须为144
        if len(features) != 144:
            print(f"警告：第{idx}个样本特征长度 {len(features)}，期望144，已跳过")
            continue
        X.append(features)
        y.append(label_map[item['label']])
    
    X = np.array(X)
    y = np.array(y)
    print(f"成功加载 {len(X)} 个样本，特征形状: {X.shape}")
    return X, y, label_map

def train_model(X, y, label_map):
    """训练模型"""
    # 划分训练集和测试集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    
    # 使用scikit-learn接口
    from lightgbm import LGBMClassifier
    
    # 计算类别权重 - 平衡训练数据分布
    from sklearn.utils.class_weight import compute_class_weight
    class_weights = compute_class_weight('balanced', classes=np.unique(y), y=y)
    class_weight_dict = {i: w for i, w in enumerate(class_weights)}
    
    print("\n类别权重:")
    label_names = {v: k for k, v in label_map.items()}
    for i, w in class_weight_dict.items():
        print(f"  {label_names[i]}: {w:.3f}")
    
    # 设置参数 - 添加类别权重
    params = {
        'boosting_type': 'gbdt',
        'objective': 'multiclass',
        'num_class': len(label_map),
        'learning_rate': 0.1,
        'num_leaves': 15,
        'max_depth': 5,
        'min_data_in_leaf': 5,
        'feature_fraction': 0.8,
        'bagging_fraction': 0.8,
        'bagging_freq': 5,
        'verbose': -1,
        'n_jobs': 1,  # 禁用多进程，使用单进程训练
        'class_weight': class_weight_dict  # 添加类别权重
    }
    
    # 训练模型
    print("\n开始训练LightGBM模型...")
    model = LGBMClassifier(**params)
    model.fit(X_train, y_train, eval_set=[(X_test, y_test)])
    
    # 模型评估
    y_pred = model.predict(X_test)
    
    print("\n模型评估结果:")
    print(f"准确率: {accuracy_score(y_test, y_pred):.4f}")
    print(classification_report(y_test, y_pred))
    
    return model

def save_model(model, label_map, model_path="pose_classifier.txt"):
    """保存模型"""
    import pickle
    
    # 使用pickle保存模型
    with open(model_path, 'wb') as f:
        pickle.dump(model, f)
    
    # 保存标签映射
    with open("label_map.json", 'w') as f:
        json.dump(label_map, f)
    
    print(f"模型已保存为 {model_path}")
    print(f"标签映射已保存为 label_map.json")

def main():
    """主函数"""
    data_file = "training_data/merged_data.json"
    
    if not os.path.exists(data_file):
        print(f"错误: 数据文件 {data_file} 不存在")
        print("请先运行 collect_data.py 收集数据，然后运行 merge_data.py 合并数据")
        return
    
    try:
        X, y, label_map = load_training_data(data_file)
        print(f"加载了 {len(X)} 个样本")
        print(f"特征向量形状: {X.shape}")
        print(f"标签形状: {y.shape}")
        print(f"标签分布: {np.bincount(y)}")
        
        model = train_model(X, y, label_map)
        save_model(model, label_map)
    except Exception as e:
        print(f"错误: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
    
