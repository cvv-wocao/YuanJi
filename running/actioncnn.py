#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
跑步姿势识别模型定义
"""

import tensorflow as tf
from keras.models import Sequential
from keras.layers import Conv2D, MaxPooling2D, Flatten, Dense, Dropout

class RunningPoseCNN:
    def __init__(self, input_shape=(64, 64, 3), num_classes=2):
        self.input_shape = input_shape
        self.num_classes = num_classes
        self.model = self.build_model()
    
    def build_model(self):
        """构建CNN模型"""
        model = Sequential([
            # 第一层卷积
            Conv2D(32, (3, 3), activation='relu', input_shape=self.input_shape),
            MaxPooling2D((2, 2)),
            
            # 第二层卷积
            Conv2D(64, (3, 3), activation='relu'),
            MaxPooling2D((2, 2)),
            
            # 第三层卷积
            Conv2D(128, (3, 3), activation='relu'),
            MaxPooling2D((2, 2)),
            
            # 扁平化
            Flatten(),
            
            # 全连接层
            Dense(128, activation='relu'),
            Dropout(0.5),
            
            # 输出层
            Dense(self.num_classes, activation='softmax' if self.num_classes > 1 else 'sigmoid')
        ])
        
        # 选择损失函数
        if self.num_classes > 1:
            loss_function = 'sparse_categorical_crossentropy'
            output_units = self.num_classes
        else:
            loss_function = 'binary_crossentropy'
            output_units = 1
        
        # 编译模型
        model.compile(
            optimizer='adam',
            loss=loss_function,
            metrics=['accuracy']
        )
        
        return model
    
    def train(self, X_train, y_train, X_test, y_test, epochs=50, batch_size=32):
        """训练模型"""
        history = self.model.fit(
            X_train,
            y_train,
            epochs=epochs,
            batch_size=batch_size,
            validation_data=(X_test, y_test),
            verbose=1
        )
        return history
    
    def predict(self, X):
        """预测"""
        return self.model.predict(X)
    
    def predict_classes(self, X):
        """预测类别"""
        predictions = self.model.predict(X)
        if self.num_classes == 1:
            return (predictions > 0.5).astype(int).flatten()
        return predictions.argmax(axis=1)
    
    def save(self, path):
        """保存模型"""
        self.model.save(path)
        print(f"模型已保存到: {path}")
    
    def load(self, path):
        """加载模型"""
        from keras.models import load_model
        self.model = load_model(path)
        print(f"模型已加载从: {path}")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='创建和查看模型结构')
    parser.add_argument('--img-size', type=int, default=64, help='输入图片大小')
    parser.add_argument('--num-classes', type=int, default=2, help='类别数量')
    args = parser.parse_args()
    
    model = RunningPoseCNN(
        input_shape=(args.img_size, args.img_size, 3),
        num_classes=args.num_classes
    )
    print(f"\n模型结构 (输入: {args.img_size}x{args.img_size}x3, 类别: {args.num_classes}):")
    model.model.summary()
