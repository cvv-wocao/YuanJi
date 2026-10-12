# 姿态分类模型训练与使用指南

本目录包含用于训练LightGBM姿态分类模型的脚本，该模型可以自动检测视频中的运动类型（跑步、哑铃弯举、俯卧撑）。

## 目录结构

```
YOLO/
├── training_data/          # 训练数据目录
├── collect_data.py         # 数据收集脚本
├── merge_data.py           # 数据合并脚本
├── train_pose_classifier.py # 模型训练脚本
├── detect_exercise_type.py # 运动类型检测脚本
├── pose_classifier.txt     # 训练好的模型文件
├── label_map.json          # 标签映射文件
└── README.md              # 本说明文件
```

## 实施步骤

### 1. 准备数据

#### 1.1 收集视频数据
- 创建视频目录结构：
  ```
  YOLO/
  └── videos/
      ├── running/          # 跑步视频
      ├── dumbbell/         # 哑铃弯举视频
      └── pushup/           # 俯卧撑视频
  ```
- 将对应运动类型的视频放入相应目录

#### 1.2 收集图片数据（可选）
- 创建图片目录结构：
  ```
  YOLO/
  └── training_images/
      ├── running/          # 跑步图片
      ├── dumbbell_curl/     # 哑铃弯举图片
      └── pushup/           # 俯卧撑图片
  ```
- 将对应运动类型的图片放入相应目录

### 2. 收集训练数据

#### 2.1 处理视频数据
- 修改 `collect_data.py` 文件，取消注释相应的函数调用
- 运行脚本：
  ```bash
  python collect_data.py
  ```

#### 2.2 处理图片数据（可选）
- 修改 `collect_data.py` 文件，取消注释相应的函数调用
- 运行脚本：
  ```bash
  python collect_data.py
  ```

### 3. 合并训练数据

- 运行脚本：
  ```bash
  python merge_data.py
  ```

### 4. 训练模型

- 运行脚本：
  ```bash
  python train_pose_classifier.py
  ```
- 训练完成后，会生成 `pose_classifier.txt` 和 `label_map.json` 文件

### 5. 检测运动类型

- 运行脚本：
  ```bash
  python detect_exercise_type.py path/to/video.mp4
  ```
- 例如：
  ```bash
  python detect_exercise_type.py videos/running/video1.mp4
  ```

## 技术说明

### 特征提取
- **关键点特征**：从MediaPipe的33个关键点中提取x, y, z, visibility四个值，共132维特征
- **角度特征**：对于跑步，额外提取膝盖、髋部、躯干角度特征

### 模型训练
- 使用LightGBM算法进行多分类训练
- 支持三种运动类型：跑步(0)、哑铃弯举(1)、俯卧撑(2)
- 模型评估指标包括准确率、精确率、召回率和F1分数

### 运动类型检测
- 对视频帧进行采样分析
- 使用投票机制确定最终运动类型
- 置信度阈值设置为0.7，确保检测结果的可靠性

## 注意事项

1. **数据质量**：确保收集的数据包含不同角度、不同光线条件下的动作
2. **数据量**：每种运动至少收集50-100个样本，以确保模型的准确性
3. **模型更新**：随着新数据的积累，定期重新训练模型
4. **阈值调整**：根据实际情况调整分类置信度阈值

## 扩展建议

1. **添加新运动类型**：
   - 在 `train_pose_classifier.py` 中更新 `label_map`
   - 收集新运动类型的数据
   - 重新训练模型

2. **模型优化**：
   - 调整LightGBM的参数以提高性能
   - 尝试其他机器学习算法
   - 增加更多特征，如速度、加速度等

3. **集成到系统**：
   - 将训练好的模型复制到项目根目录
   - 修改 `api_server.py`，添加自动运动类型检测功能
