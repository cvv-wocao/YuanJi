#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
跑步姿势识别应用 - 整合Web界面和直接运行模式
"""

import os
import sys
import cv2
import time
import traceback
import threading
import logging
import numpy as np
from pathlib import Path
from flask import Flask, render_template, Response, request, jsonify
from flask_cors import CORS  # 必须安装
from collections import deque
from RunningPoseModule import RunningPoseDetector

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 初始化 Flask
app = Flask(__name__)

# -------------- 【核心：正确跨域配置，解决你现在的报错】--------------
CORS(app, resources={
    r"/api/*": {
        "origins": "*",
        "methods": ["GET", "POST", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

# 全局变量
camera_lock = threading.Lock()
current_frame = None
frame_buffer = deque(maxlen=10)
is_running = True

class UltraStableRunningPoseDetector:
    """超级鲁棒的跑步姿势检测器"""
    
    def __init__(self):
        self.detector = None
        self.initialized = False
        self.retry_count = 0
        self.max_retries = 3
        
        # 备用检测器标志
        self.use_fallback = False
        
        logger.info("初始化超级鲁棒跑步姿势检测器...")
        self._initialize_detector()
    
    def _initialize_detector(self):
        """初始化检测器"""
        try:
            # 尝试初始化MediaPipe姿势检测器
            self.detector = RunningPoseDetector()
            self.initialized = True
            self.use_fallback = False
            logger.info("MediaPipe跑步姿势检测器初始化成功")
            
        except Exception as e:
            logger.error(f"MediaPipe初始化失败: {e}")
            self.initialized = False
            self.use_fallback = True
            logger.info("使用备用检测模式")
    
    def process_frame_safe(self, frame):
        """安全处理帧"""
        if not self.initialized and self.use_fallback:
            # 备用模式：简单的人体检测
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            blur = cv2.GaussianBlur(gray, (5, 5), 0)
            _, thresh = cv2.threshold(blur, 100, 255, cv2.THRESH_BINARY)
            
            cv2.putText(frame, "FALLBACK MODE", (10, 30), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
            cv2.putText(frame, "No pose detection", (10, 70), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
            return frame
        
        try:
            # 使用MediaPipe处理
            frame, posture, score = self.detector.process_frame(frame)
            return frame
        except Exception as e:
            logger.error(f"处理帧时出错: {e}")
            # 出错时返回原始帧
            cv2.putText(frame, "Error in processing", (10, 70), 
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
            return frame

def generate_stable_frames():
    """生成稳定的视频帧"""
    global is_running
    detector = UltraStableRunningPoseDetector()
    
    # 初始化摄像头
    cap = None
    retry_count = 0
    max_retries = 5
    
    while retry_count < max_retries:
        try:
            cap = cv2.VideoCapture(0)
            if cap.isOpened():
                cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                logger.info("摄像头初始化成功")
                break
            else:
                logger.error("无法打开摄像头")
                retry_count += 1
                time.sleep(1)
        except Exception as e:
            logger.error(f"摄像头初始化失败: {e}")
            retry_count += 1
            time.sleep(1)
    
    if cap is None or not cap.isOpened():
        logger.error("无法初始化摄像头，使用静态图像")
        # 使用静态图像作为 fallback
        static_frame = cv2.imread("static_placeholder.jpg")
        if static_frame is None:
            static_frame = cv2.imread("placeholder.jpg")
        if static_frame is None:
            # 创建一个简单的占位图像
            static_frame = cv2.zeros((480, 640, 3), dtype=np.uint8)
            cv2.putText(static_frame, "Camera not available", (100, 200), 
                       cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        
        while is_running:
            ret, buffer = cv2.imencode('.jpg', static_frame)
            if not ret:
                continue
            frame = buffer.tobytes()
            yield (b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')
            time.sleep(0.1)
        return
    
    # 主循环
    frame_count = 0
    start_time = time.time()
    
    try:
        while is_running:
            with camera_lock:
                ret, frame = cap.read()
            
            if not ret:
                logger.warning("无法读取帧，使用缓冲帧")
                if frame_buffer:
                    frame = frame_buffer[-1].copy()
                else:
                    # 创建空白帧
                    frame = cv2.zeros((480, 640, 3), dtype=np.uint8)
                    cv2.putText(frame, "No frame available", (100, 200), 
                               cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
            
            # 镜像翻转
            frame = cv2.flip(frame, 1)
            
            # 处理帧
            processed_frame = detector.process_frame_safe(frame)
            
            # 保存到缓冲
            frame_buffer.append(processed_frame.copy())
            
            # 编码帧
            ret, buffer = cv2.imencode('.jpg', processed_frame)
            if not ret:
                logger.warning("无法编码帧")
                continue
            
            frame_bytes = buffer.tobytes()
            
            # 生成MJPEG流
            yield (b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')
            
            # FPS控制
            frame_count += 1
            if frame_count % 30 == 0:
                elapsed = time.time() - start_time
                fps = 30 / elapsed if elapsed > 0 else 0
                start_time = time.time()
                logger.info(f"FPS: {fps:.1f}")
                
            # 适当延迟
            time.sleep(0.03)
            
    except KeyboardInterrupt:
        logger.info("程序被用户中断")
    except Exception as e:
        logger.error(f"生成帧时出错: {e}")
        logger.error(traceback.format_exc())
    finally:
        if cap is not None:
            cap.release()
        logger.info("摄像头资源已释放")

def run_gesture_test():
    """运行跑步姿势识别测试 - 直接窗口显示"""
    print("=== 跑步姿势识别测试 ===")
    print("按 'q' 退出，按 's' 保存截图")
    
    # 初始化检测器
    detector = UltraStableRunningPoseDetector()
    
    # 初始化摄像头
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("❌ 无法打开摄像头")
        return
    
    # 设置摄像头参数
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    
    print("✅ 摄像头初始化成功")
    print("🔍 检测器状态:", "OK" if detector.initialized else "FALLBACK")
    
    frame_count = 0
    start_time = time.time()
    
    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("❌ 无法读取帧")
                break
            
            # 镜像翻转
            frame = cv2.flip(frame, 1)
            
            try:
                # 处理帧
                img = detector.process_frame_safe(frame)
                
                # 状态指示
                status_color = (0, 255, 0) if detector.initialized else (0, 0, 255)
                cv2.putText(img, f"Status: {'OK' if detector.initialized else 'FALLBACK'}", 
                           (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, status_color, 2)
                
                # FPS计算
                frame_count += 1
                if frame_count % 30 == 0:
                    elapsed = time.time() - start_time
                    fps = 30 / elapsed if elapsed > 0 else 0
                    start_time = time.time()
                    print(f"📊 FPS: {fps:.1f}")
                
            except Exception as e:
                print(f"❌ 处理帧时出错: {e}")
                # 仍然显示原始帧
                cv2.putText(img, "Error in processing", (10, 70),
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
            
            # 显示窗口
            cv2.imshow("跑步姿势识别测试", img)
            
            # 键盘控制
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                print("👋 用户退出")
                break
            elif key == ord('s'):
                # 保存截图
                filename = f"running_test_{time.strftime('%Y%m%d_%H%M%S')}.jpg"
                cv2.imwrite(filename, img)
                print(f"📸 截图已保存: {filename}")
    
    except KeyboardInterrupt:
        print("\n👋 程序被用户中断")
    except Exception as e:
        print(f"❌ 程序错误: {e}")
        print(traceback.format_exc())
    finally:
        cap.release()
        cv2.destroyAllWindows()
        print("✅ 程序正常退出")

@app.route('/')
def index():
    """主页"""
    return render_template('camera.html')

@app.route('/video')
def video():
    """视频流"""
    return Response(generate_stable_frames(),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    # 获取运行模式参数
    import argparse
    parser = argparse.ArgumentParser(description='跑步姿势识别应用')
    parser.add_argument('--mode', choices=['web', 'direct'], default='web',
                       help='运行模式: web=web界面, direct=直接窗口显示')
    args = parser.parse_args()
    
    if args.mode == 'direct':
        # 直接运行窗口版本
        run_gesture_test()
    else:
        # 运行Web版本
        logger.info("启动超级鲁棒跑步姿势识别Web应用...")
        logger.info("访问 http://localhost:5002 查看应用")
        
        try:
            app.run(host='0.0.0.0', port=5002, debug=True, threaded=True)
        except KeyboardInterrupt:
            logger.info("程序被用户中断")
        except Exception as e:
            logger.error(f"应用启动失败: {e}")
        finally:
            is_running = False
