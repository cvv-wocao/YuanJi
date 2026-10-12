#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查训练数据的分布情况
"""

import json
import os

def check_data_distribution():
    """检查训练数据的分布情况"""
    data_file = "training_data/merged_data.json"
    
    if not os.path.exists(data_file):
        print(f"错误: 数据文件 {data_file} 不存在")
        return
    
    # 加载数据
    with open(data_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"总样本数: {len(data)}")
    
    # 统计每种运动类型的样本数
    label_counts = {}
    for item in data:
        label = item.get('label', 'unknown')
        if label not in label_counts:
            label_counts[label] = 0
        label_counts[label] += 1
    
    print("\n各运动类型样本分布:")
    for label, count in label_counts.items():
        percentage = (count / len(data)) * 100
        print(f"  {label}: {count}个样本 ({percentage:.2f}%)")
    
    # 检查特征维度
    if data:
        first_features = data[0].get('features', [])
        print(f"\n特征维度: {len(first_features)}")
        print(f"第一个样本特征长度: {len(first_features)}")
    
if __name__ == "__main__":
    check_data_distribution()
