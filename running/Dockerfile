FROM python:3.12-slim
WORKDIR /app

# OpenCV / MediaPipe 运行需要的系统库（libegl1 / libgles2 是 MediaPipe 必需的）
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgl1 libglib2.0-0 libgomp1 \
        libegl1 libgles2 \
        libsm6 libxext6 libxrender1 libx11-6 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# 先装 CPU 版 torch / torchvision：避免 ultralytics 默认拉取体积巨大的 CUDA 版本。
# 需要 GPU 推理时，删掉 --index-url 参数即可（镜像会大好几 GB）。
# pip 缓存挂载可以显著加快重复构建。
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu

RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt

COPY . .

# RunningPoseModule 按 <项目根>/YOLO/best.pt 查找 YOLO 模型，
# 容器里的项目根是 /，所以预留 /YOLO，由 docker-compose 挂载模型进去。
RUN mkdir -p /YOLO

EXPOSE 5002
CMD ["python", "api_server.py"]