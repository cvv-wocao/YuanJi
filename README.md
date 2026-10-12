# 远疾（yuanji）智能健身跑步平台

一个集 **移动端 App + Web 管理端 + 后端服务 + AI 姿态分析 + 大数据推荐** 于一体的智能健身 / 跑步系统。

覆盖业务：社区动态（发帖 / 评论 / 点赞 / 关注）、运动商城（商品 / 购物车 / 优惠券 / 订单 / 退款）、消息、设备远程监控（树莓派）、AI 运动姿态分析与实时反馈、运营数据大屏。

---

## 一、系统架构

| 模块 | 目录 | 职责 | 技术栈 |
| --- | --- | --- | --- |
| 后端服务 | `springboot/Demo35` | 业务 API、鉴权、支付、WebSocket、消息推送 | Java 21 · Spring Boot 3.2.4 · MyBatis · MySQL · Redis · Kafka · HBase · JWT |
| 管理端 / Web 用户端 | `vue_admin/Demo35` | 运营后台 + 数据大屏 + Web 端用户界面 | Vue 3 · Vite 7 · Element Plus · ECharts · axios |
| 用户端 App | `uniapp_user/yuanjiapp` | 手机端 App（可打包 H5 / APK） | uni-app（Vue 3） |
| AI 姿态分析服务 | `running` | 视频 / 单帧姿态打分、动作计数、实时反馈 | Python 3.12 · Flask · MediaPipe · OpenCV · PyTorch |
| 运动类型识别 | `YOLO` | 自动判别跑步 / 哑铃弯举 / 俯卧撑 | MediaPipe 关键点 + LightGBM |
| 大数据离线计算 | `scala` | 帖子推荐模型训练与产出、Hive 数据同步 | Spark MLlib(ALS) · Spark SQL · Hive · HBase · Redis |

### 数据流

```
                       ┌──────────────────────────────┐
  uni-app 用户端 ──────▶│                              │
  Vue 管理端 / Web ────▶│   Spring Boot 后端 (:8080)    │────▶ MySQL (yuanji)
                       │                              │────▶ Redis（推荐结果缓存）
  树莓派 / 摄像头 ─────▶│                              │
                       └───────────┬──────────────────┘
                                   │ 视频上传 OSS 后 POST /api/video/analyze
                                   ▼
                        Python AI 服务 (:5002)  ←  MediaPipe / YOLO 模型
                                   │ 分析结果
                                   ▼
                            回写后端 → 前端展示

  业务库变更 ──▶ Kafka ──▶ KafkaConsumerService ──▶ WebSocket ──▶ 数据大屏
  MySQL/Hive ──▶ Spark ALS 训练 ──▶ HDFS 模型 ──▶ PostRecommender ──▶ Redis ──▶ 前端
```

---

## 二、目录结构

```
yuanji/
├── .env                     # ← 全部密钥（本地，已被 .gitignore 忽略，不入库）
├── .env.example             # 密钥模板，复制为 .env 后填值
├── docker-compose.yml       # Docker 编排（env_file: .env）
├── DOCKER.md                # 敏感配置 + Docker 使用说明
├── springboot/Demo35/       # Spring Boot 后端
├── vue_admin/Demo35/        # Vue 3 管理端 / Web 端
├── uniapp_user/yuanjiapp/   # uni-app 用户端
├── running/                 # Python AI 姿态分析服务
├── YOLO/                    # 运动类型识别模型训练
└── scala/                   # Spark 推荐 / Hive 同步脚本
```

> 注意：`YOLO/training_images/**` 训练图片、`YOLO/videos/**` 训练视频等素材未纳入仓库，需自行准备后再运行 AI 相关功能。

---

## 三、快速开始

### 0. 准备密钥配置（必做）

所有敏感信息集中在根目录 `.env`，仓库里只有模板：

```bash
cp .env.example .env          # Windows: copy .env.example .env
```

然后编辑 `.env` 填入真实值（MySQL、JWT、支付宝、OSS、AnythingLLM、讯飞星火等）。
Java 与 Python 都会自动读取它，详见 `DOCKER.md`。

### 1. 初始化数据库

- **MySQL**：建库 `yuanji`（字符集 `utf8mb4`），并导入表结构。
  仓库内仅有 `springboot/Demo35/src/main/resources/schema/long_term_memory.sql` 一个建表脚本，其余表需自行导出/导入。
- **SQLite**：Python 服务首次运行时会自动建表（`running/running.db`），无需手工处理。

### 2. 启动后端（端口 8080）

```bash
cd springboot/Demo35
mvn spring-boot:run            # 或 mvn clean package 后 java -jar target/*.jar
```

### 3. 启动 AI 姿态分析服务（端口 5002）

```bash
cd running
python -m venv .venv           # 首次
.venv\Scripts\activate         # Windows
pip install -r requirements.txt
python api_server.py
```

主要接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/analyze/frame` | 单帧图片分析（实时反馈） |
| GET | `/api/analyze/standard` | 获取标准动作参考 |
| POST | `/api/analyze/video` | 整段视频分析 |
| GET | `/api/records` | 运动记录 |
| GET | `/api/reports/{id}` | 分析报告详情 |

### 4. 启动管理端 / Web 端（端口 3000）

```bash
cd vue_admin/Demo35
npm install
npm run dev
```

开发服务器已配置代理：`/api` → `http://localhost:8080`。

### 5. 启动用户端（uni-app）

```bash
cd uniapp_user/yuanjiapp
npm install
npm run dev:h5                 # 或 npm run build:h5
```

后端地址在 `utils/request.js` 的 `BASE_URL` 中配置（默认 `http://localhost:8080`）。

### 6. Docker（可选）

```bash
docker compose --env-file .env up -d
```

后端与 AI 服务已提供 Dockerfile，编排包含 mysql / redis / ai / backend 四个服务。密钥通过 `env_file` 注入，不会进入镜像。

---

## 四、端口一览

| 服务 | 端口 |
| --- | --- |
| Spring Boot 后端 | 8080 |
| Python AI 服务 | 5002 |
| Vue 管理端（dev） | 3000 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 9092 |
| HBase / ZooKeeper | 2181 |
| AnythingLLM | 3001 |

---

## 五、大数据模块说明

Kafka → WebSocket 的数据大屏链路和 Spark 推荐链路默认**关闭**，由配置开关控制：

```yaml
bigdata:
  enabled: false   # 改为 true 启用（需先准备好 Kafka / HBase / Redis 集群）
```

- 大屏：`KafkaConsumerService` 消费 `user-change`、`post-change`、`product-change`、`category-change`、`order-change`，通过 WebSocket 广播给前端。
- 推荐：`scala` 下 `PostTraining` 训练 ALS 模型（网格搜索，按 AUC 选优，模型路径存 MySQL `model_params`），`PostRecommender` 加载最优模型生成推荐并写入 Redis（`rec:post:user:*`，7 天过期）。

---

## 六、常见问题

- **后端启动报缺少变量**：检查根目录 `.env` 是否已创建、是否缺少 `JWT_SECRET` / `DB_PASSWORD` / `ALIPAY_*` / `OSS_*` 等必填项。若启动目录不是 `springboot/Demo35`，需用 `YUANJI_ENV_FILE` 指定 `.env` 绝对路径。
- **Python 提示 `环境变量 JWT_SECRET 未设置`**：同上，确认 `.env` 在项目根目录，或直接用环境变量注入。
- **Java 调用 Python 服务失败**：确认 `PYTHON_API_URL` 正确（默认 `http://localhost:5002/api/analyze/frame`，Docker 下为 `http://ai:5002/api/analyze/frame`），并已启动 AI 服务。
- **模型 / 数据集缺失**：`*.task` 姿态模型、`*.pt`、`YOLO/training_images/**` 训练图片、`YOLO/videos/**` 与 `running/temp_videos/` 视频等文件未纳入仓库，需自行准备后再运行 AI 相关功能。

---

## 七、安全须知

- 密钥只放在根目录 `.env`，该文件已被 `.gitignore` 忽略，**请勿提交**。
- 如果曾把密钥提交进 Git 历史，仅加 `.gitignore` 无效，必须清理历史并**轮换全部密钥**。
- 请勿把真实的生产密钥、数据库密码、支付私钥写入任何入库文件。