"""统一读取敏感配置。

优先级：已存在的系统环境变量 > 项目根目录下的 .env 文件。

- 本地运行：自动向上查找并加载项目根目录的 .env（该文件已被 .gitignore
  忽略，不会上传到 GitHub）。
- Docker 运行：直接使用容器注入的环境变量，.env 可挂载也可不挂载。

用法：
    from config import get, require
    secret = require("JWT_SECRET")
"""
import os
from pathlib import Path


def _load_dotenv():
    here = Path(__file__).resolve().parent
    for base in (here, *here.parents):
        env_file = base / ".env"
        if not env_file.is_file():
            continue
        for raw_line in env_file.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key:
                # 不覆盖已存在的环境变量（Docker 注入的值优先）
                os.environ.setdefault(key, value)
        return


_load_dotenv()


def get(name, default=None):
    return os.environ.get(name, default)


def require(name):
    value = os.environ.get(name)
    if not value:
        raise RuntimeError(
            "环境变量 %s 未设置，请在项目根目录的 .env 中配置（参考 .env.example）" % name
        )
    return value