# 敏感配置与 Docker 使用说明

## 1. 唯一真源：根目录 `.env`

所有密钥/密码只集中放在项目根目录的 `.env` 里，模板见 `.env.example`。

```bash
cp .env.example .env        # Windows: copy .env.example .env
# 编辑 .env，填入真实值
```

`.env` 已被根目录 `.gitignore` 忽略，**不会**上传到 GitHub。

> 注意：如果这些密钥之前已经提交过，`.gitignore` 只是让 Git 不再跟踪，
> 历史提交里依然存在。必须清理历史（git filter-repo / BFG）并轮换密钥。

## 2. 本地运行

- Java（Spring Boot）：`application.yml` 通过
  `spring.config.import: optional:file:../../.env[.properties]` 自动加载根目录 `.env`。
  在 `springboot/Demo35` 目录下启动即可。
- Python（`running/`）：`running/config.py` 会自动向上查找并加载根目录 `.env`。

## 3. Docker（三种方式，都指向同一个 `.env`）

### 方式 A：docker compose 的 env_file（推荐）

```yaml
services:
  backend:
    env_file:
      - .env
```

启动：`docker compose --env-file .env up -d`
根目录 `docker-compose.yml` 已配好 mysql / redis / ai / backend 四个服务，直接 `docker compose up -d --build` 即可。

### 方式 B：docker run --env-file

```bash
docker run --env-file .env -p 8080:8080 rumbing-backend
```

### 方式 C：Docker secrets（生产推荐）

```yaml
services:
  backend:
    secrets: [jwt_secret]
secrets:
  jwt_secret:
    file: ./secrets/jwt_secret.txt
```

把 `secrets/` 加入 `.gitignore`，容器内从 `/run/secrets/<name>` 读取。

## 4. 容器里读不到 .env 时

用环境变量指定路径，或直接把 `.env` 挂进容器：

```bash
docker run -e YUANJI_ENV_FILE=/app/.env -v "$PWD/.env:/app/.env:ro" ...
```

更简单的是用方式 A/B，让 Docker 注入环境变量（优先级高于 `.env` 文件本身）。

## 5. 变量清单

| 变量 | 说明 |
| --- | --- |
| `DB_USERNAME` / `DB_PASSWORD` / `DB_URL` | MySQL |
| `JWT_SECRET` / `JWT_EXPIRATION` | JWT，Java 与 Python 必须一致 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka |
| `HBASE_ZK_QUORUM` / `HBASE_ZK_PORT` | HBase |
| `ALIPAY_APP_ID` / `ALIPAY_MERCHANT_PRIVATE_KEY` / `ALIPAY_PUBLIC_KEY` | 支付宝沙箱 |
| `ALIPAY_GATEWAY_URL` / `ALIPAY_NOTIFY_URL` / `ALIPAY_RETURN_URL` | 支付宝回调地址 |
| `OSS_ENDPOINT` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BUCKET` | 阿里云 OSS |
| `ANYTHINGLLM_BASE_URL` / `ANYTHINGLLM_API_KEY` / `ANYTHINGLLM_WORKSPACE_SLUG` | AnythingLLM |
| `SPARK_APP_ID` / `SPARK_API_KEY` / `SPARK_API_SECRET` / `SPARK_URL` / `SPARK_DOMAIN` | 讯飞星火 |