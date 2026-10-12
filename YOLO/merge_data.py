#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
合并训练数据
"""

import json
import os

def merge_data(input_dir, output_file):
    """合并所有训练数据"""
    merged_data = []
    
    for file in os.listdir(input_dir):
        if file.endswith('.json'):
            file_path = os.path.join(input_dir, file)
            with open(file_path, 'r') as f:
                data = json.load(f)
                merged_data.extend(data)
    
    # 保存合并后的数据
    with open(output_file, 'w') as f:
        json.dump(merged_data, f)
    
    print(f"合并了 {len(merged_data)} 个样本，保存到 {output_file}")

if __name__ == "__main__":
    merge_data("training_data", "training_data/merged_data.json")
