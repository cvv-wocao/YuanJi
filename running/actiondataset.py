#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
跑步姿势数据集处理 - 支持图片和视频
支持两种目录结构：
1. 直接结构: datasets/img, datasets/videos
2. 分类结构: datasets/good/img, datasets/good/videos, datasets/bad/img, datasets/bad/videos
"""

import os
import cv2
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split

class RunningPoseDataset:
    def __init__(self, data_folder, img_size=(64, 64), use_video_frames=5):
        self.data_folder = Path(data_folder)
        self.img_size = img_size
        self.use_video_frames = use_video_frames
        
        # 自动检测目录结构
        self._detect_structure()
    
    def _detect_structure(self):
        """检测数据集目录结构"""
        # 检查是否有分类文件夹
        good_folder = self.data_folder / 'good'
        bad_folder = self.data_folder / 'bad'
        
        if good_folder.exists() or bad_folder.exists():
            self.categories = ['good', 'bad']
            self.is_categorized = True
        else:
            # 检查是否有 img 和 videos 文件夹（无分类）
            img_folder = self.data_folder / 'img'
            videos_folder = self.data_folder / 'videos'
            
            if img_folder.exists() or videos_folder.exists():
                # 无分类结构
                self.categories = ['all']
                self.is_categorized = False
            else:
                self.categories = ['all']
                self.is_categorized = False
        
        self.category_to_label = {cat: i for i, cat in enumerate(self.categories)}
    
    def load_images(self, folder):
        """加载图片数据集"""
        images = []
        
        img_folder = folder / 'img'
        if not img_folder.exists():
            return images
        
        image_extensions = ['.jpg', '.jpeg', '.png', '.bmp']
        for image_path in img_folder.glob('*.*'):
            if image_path.suffix.lower() in image_extensions:
                img = cv2.imread(str(image_path))
                if img is None:
                    continue
                
                img = cv2.resize(img, self.img_size)
                img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
                img = img / 255.0
                images.append(img)
        
        return images
    
    def load_videos(self, folder):
        """加载视频数据集"""
        videos = []
        
        videos_folder = folder / 'videos'
        if not videos_folder.exists():
            return videos
        
        video_files = list(videos_folder.glob('*.mp4'))
        
        for video_path in video_files:
            try:
                cap = cv2.VideoCapture(str(video_path))
                if not cap.isOpened():
                    continue
                
                frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
                
                frames_to_extract = min(self.use_video_frames, frame_count)
                if frame_count > 0:
                    frame_indices = np.linspace(0, frame_count - 1, frames_to_extract, dtype=int)
                else:
                    frame_indices = []
                
                for idx in frame_indices:
                    cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
                    ret, frame = cap.read()
                    if not ret:
                        continue
                    
                    frame = cv2.resize(frame, self.img_size)
                    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    frame = frame / 255.0
                    videos.append(frame)
                
                cap.release()
            except Exception as e:
                print(f"加载视频失败 {video_path}: {e}")
                continue
        
        return videos
    
    def load_dataset(self):
        """加载完整数据集（图片 + 视频）"""
        images = []
        labels = []
        
        if self.is_categorized:
            # 分类结构: datasets/good/img, datasets/good/videos
            for category in self.categories:
                category_folder = self.data_folder / category
                if not category_folder.exists():
                    continue
                
                category_images = self.load_images(category_folder)
                for img in category_images:
                    images.append(img)
                    labels.append(self.category_to_label[category])
                
                category_videos = self.load_videos(category_folder)
                for frame in category_videos:
                    images.append(frame)
                    labels.append(self.category_to_label[category])
        else:
            # 直接结构: datasets/img, datasets/videos
            category_images = self.load_images(self.data_folder)
            for img in category_images:
                images.append(img)
                labels.append(0)  # 统一标记为0
            
            category_videos = self.load_videos(self.data_folder)
            for frame in category_videos:
                images.append(frame)
                labels.append(0)  # 统一标记为0
        
        return np.array(images), np.array(labels)
    
    def load_image_dataset_only(self):
        """仅加载图片数据集"""
        images = []
        labels = []
        
        if self.is_categorized:
            for category in self.categories:
                category_folder = self.data_folder / category
                if not category_folder.exists():
                    continue
                
                category_images = self.load_images(category_folder)
                for img in category_images:
                    images.append(img)
                    labels.append(self.category_to_label[category])
        else:
            category_images = self.load_images(self.data_folder)
            for img in category_images:
                images.append(img)
                labels.append(0)
        
        return np.array(images), np.array(labels)
    
    def load_video_dataset_only(self):
        """仅加载视频数据集"""
        images = []
        labels = []
        
        if self.is_categorized:
            for category in self.categories:
                category_folder = self.data_folder / category
                if not category_folder.exists():
                    continue
                
                category_videos = self.load_videos(category_folder)
                for frame in category_videos:
                    images.append(frame)
                    labels.append(self.category_to_label[category])
        else:
            category_videos = self.load_videos(self.data_folder)
            for frame in category_videos:
                images.append(frame)
                labels.append(0)
        
        return np.array(images), np.array(labels)
    
    def get_train_test_split(self, test_size=0.2, random_state=42, use_video=True):
        """获取训练集和测试集"""
        if use_video:
            images, labels = self.load_dataset()
        else:
            images, labels = self.load_image_dataset_only()
        
        if len(images) == 0:
            raise ValueError("没有找到任何训练数据！请检查数据集目录结构。")
        
        return train_test_split(images, labels, test_size=test_size, random_state=random_state)
    
    def get_dataset_info(self):
        """获取数据集信息"""
        info = {
            'images_count': 0,
            'videos_count': 0,
            'frames_from_videos': 0,
            'total_samples': 0,
            'category_counts': {},
            'structure': 'categorized' if self.is_categorized else 'direct'
        }
        
        if self.is_categorized:
            for category in self.categories:
                category_folder = self.data_folder / category
                if not category_folder.exists():
                    continue
                
                # 加载图片
                img_folder = category_folder / 'img'
                if img_folder.exists():
                    images = list(img_folder.glob('*.*'))
                    images = [f for f in images if f.suffix.lower() in ['.jpg', '.jpeg', '.png', '.bmp']]
                    info['images_count'] += len(images)
                
                # 加载视频
                videos_folder = category_folder / 'videos'
                if videos_folder.exists():
                    videos = list(videos_folder.glob('*.mp4'))
                    info['videos_count'] += len(videos)
                    
                    for video in videos:
                        try:
                            cap = cv2.VideoCapture(str(video))
                            if cap.isOpened():
                                frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
                                info['frames_from_videos'] += min(self.use_video_frames, frame_count)
                                cap.release()
                        except:
                            continue
                
                info['category_counts'][category] = info['images_count'] + info['frames_from_videos']
        else:
            # 直接结构
            img_folder = self.data_folder / 'img'
            if img_folder.exists():
                images = list(img_folder.glob('*.*'))
                images = [f for f in images if f.suffix.lower() in ['.jpg', '.jpeg', '.png', '.bmp']]
                info['images_count'] = len(images)
            
            videos_folder = self.data_folder / 'videos'
            if videos_folder.exists():
                videos = list(videos_folder.glob('*.mp4'))
                info['videos_count'] = len(videos)
                
                for video in videos:
                    try:
                        cap = cv2.VideoCapture(str(video))
                        if cap.isOpened():
                            frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
                            info['frames_from_videos'] += min(self.use_video_frames, frame_count)
                            cap.release()
                    except:
                        continue
            
            info['category_counts']['all'] = info['images_count'] + info['frames_from_videos']
        
        info['total_samples'] = info['images_count'] + info['frames_from_videos']
        return info

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='数据集信息查看')
    parser.add_argument('--folder', type=str, default='datasets', help='数据集文件夹路径')
    parser.add_argument('--img-size', type=int, default=64, help='图片大小')
    parser.add_argument('--video-frames', type=int, default=5, help='每个视频提取的帧数')
    args = parser.parse_args()
    
    dataset = RunningPoseDataset(args.folder, img_size=(args.img_size, args.img_size), use_video_frames=args.video_frames)
    
    print("=== 数据集信息 ===")
    print(f"检测到的目录结构: {dataset.categories}")
    print(f"结构类型: {dataset.is_categorized and '分类' or '直接'}")
    
    info = dataset.get_dataset_info()
    print(f"图片数量: {info['images_count']}")
    print(f"视频数量: {info['videos_count']}")
    print(f"从视频提取的帧数: {info['frames_from_videos']}")
    print(f"总样本数: {info['total_samples']}")
    print(f"各类别数量: {info['category_counts']}")
    
    if info['total_samples'] > 0:
        print("\n=== 加载数据集 ===")
        try:
            X_train, X_test, y_train, y_test = dataset.get_train_test_split()
            print(f"训练集: {X_train.shape}, 测试集: {X_test.shape}")
            print(f"训练标签分布: {np.unique(y_train, return_counts=True)}")
            print(f"测试标签分布: {np.unique(y_test, return_counts=True)}")
        except Exception as e:
            print(f"加载数据集时出错: {e}")
    else:
        print("\n警告: 没有找到训练数据！")
        print("请确保数据集目录结构正确：")
        print("  直接结构: datasets/img/ - 图片, datasets/videos/ - 视频")
        print("  分类结构: datasets/good/img/, datasets/bad/img/ 等")
