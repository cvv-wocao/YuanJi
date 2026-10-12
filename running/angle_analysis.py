import cv2
import mediapipe as mp
import numpy as np
import matplotlib.pyplot as plt
from collections import deque

# ------------------------------
# 1. 初始化 MediaPipe Pose
# ------------------------------
mp_pose = mp.solutions.pose
pose = mp_pose.Pose(
    static_image_mode=False,
    model_complexity=1,      # 0,1,2 精度递增，速度递减
    smooth_landmarks=True,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

# ------------------------------
# 2. 定义角度计算函数
# ------------------------------
def calculate_angle(a, b, c):
    """计算三点夹角（度）
    a, b, c 为 (x, y) 坐标，b 是顶点
    """
    a = np.array(a)
    b = np.array(b)
    c = np.array(c)

    rad = np.arctan2(c[1] - b[1], c[0] - b[0]) - np.arctan2(a[1] - b[1], a[0] - b[0])
    angle = np.abs(rad * 180.0 / np.pi)
    if angle > 180.0:
        angle = 360 - angle
    return angle

# ------------------------------
# 3. 处理视频
# ------------------------------
video_path = "E:\跑步系统项目3.0\跑步系统项目\running\datasets\videos\running.mp4"          # 替换为你的视频路径
cap = cv2.VideoCapture(video_path)

if not cap.isOpened():
    print("无法打开视频")
    exit()

# 获取视频帧率，用于将帧数转换为时间
fps = cap.get(cv2.CAP_PROP_FPS)
frame_count = 0

angles = []      # 存储角度值
timestamps = []  # 存储时间（秒）

# 可选：显示实时画面的窗口
cv2.namedWindow("Pose Analysis", cv2.WINDOW_NORMAL)

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break

    # 转换颜色空间（MediaPipe 需要 RGB）
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = pose.process(rgb_frame)

    # 如果检测到姿态
    if results.pose_landmarks:
        # 提取关键点坐标（归一化坐标，x,y 范围 0~1）
        landmarks = results.pose_landmarks.landmark

        # 获取肩、肘、腕的索引（MediaPipe Pose 标准）
        # 右肩 12，右肘 14，右腕 16
        # 左肩 11，左肘 13，左腕 15
        # 此处以右手为例
        try:
            shoulder = [landmarks[12].x, landmarks[12].y]
            elbow    = [landmarks[14].x, landmarks[14].y]
            wrist    = [landmarks[16].x, landmarks[16].y]

            # 计算角度
            angle = calculate_angle(shoulder, elbow, wrist)

            # 记录
            angles.append(angle)
            timestamp = frame_count / fps
            timestamps.append(timestamp)

            # 可选：在图像上绘制关键点和角度
            h, w, _ = frame.shape
            # 将归一化坐标转换为像素坐标
            def to_pixel(landmark):
                return int(landmark[0] * w), int(landmark[1] * h)

            sh_px = to_pixel(shoulder)
            el_px = to_pixel(elbow)
            wr_px = to_pixel(wrist)

            cv2.line(frame, sh_px, el_px, (0, 255, 0), 2)
            cv2.line(frame, el_px, wr_px, (0, 255, 0), 2)
            cv2.circle(frame, sh_px, 5, (0, 0, 255), -1)
            cv2.circle(frame, el_px, 5, (0, 0, 255), -1)
            cv2.circle(frame, wr_px, 5, (0, 0, 255), -1)

            cv2.putText(frame, f"Elbow angle: {int(angle)}", (50, 50),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 0), 2)

        except IndexError:
            # 某些帧可能关键点缺失
            pass

    # 显示帧（可选）
    cv2.imshow("Pose Analysis", frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

    frame_count += 1

# 释放资源
cap.release()
cv2.destroyAllWindows()
pose.close()

# ------------------------------
# 4. 绘制角度-时间曲线
# ------------------------------
if angles:
    plt.figure(figsize=(12, 5))
    plt.plot(timestamps, angles, 'b-', linewidth=2)
    plt.xlabel("Time (s)")
    plt.ylabel("Elbow Angle (deg)")
    plt.title("Elbow Angle vs Time")
    plt.grid(True)
    plt.ylim(0, 180)
    plt.show()
else:
    print("未检测到任何姿态数据")