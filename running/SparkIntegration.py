import json
import time
from X1_ws import main as spark_main

from config import get

# 星火大模型配置（统一从 .env / 环境变量读取）
APPID = get("SPARK_APP_ID", "")
API_KEY = get("SPARK_API_KEY", "")
API_SECRET = get("SPARK_API_SECRET", "")
SPARK_URL = get("SPARK_URL", "wss://spark-api.xf-yun.com/v1/x1")
DOMAIN = get("SPARK_DOMAIN", "x1")

class SparkIntegration:
    def __init__(self):
        self.last_call_time = 0
        self