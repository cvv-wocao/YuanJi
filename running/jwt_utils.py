import jwt
import time

from config import require

# 与 Spring Boot 保持一致的密钥，统一从 .env / 环境变量读取
JWT_SECRET = require("JWT_SECRET")
JWT_ALGORITHM = 'HS256'

def get_username_from_token(token=None):
    """
    解析 Spring Boot 生成的 JWT token，提取用户名
    如果 token 为空，则从 Flask 请求头中获取
    如果 token 无效或解析失败，返回 None
    """
    try:
        # 如果没有提供 token，尝试从 Flask 请求中获取
        if token is None:
            try:
                from flask import request
                auth_header = request.headers.get('Authorization')
                if not auth_header:
                    print("未提供 Token")
                    return None
                # 去掉 "Bearer " 前缀（如果存在）
                if auth_header.startswith('Bearer '):
                    token = auth_header[7:]
                else:
                    token = auth_header
            except Exception as e:
                print(f"从请求中获取 Token 失败: {e}")
                return None

        print(f"收到的token: {token}")

        # 解析 JWT token（不验证签名，只提取信息）
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM], options={"verify_signature": False})
        username = payload.get('sub')

        if not username:
            print("Token 中没有用户名")
            return None

        print(f"从 Token 中提取用户名: {username}")
        return username

    except jwt.ExpiredSignatureError:
        print("Token 已过期")
        return None
    except jwt.InvalidTokenError as e:
        print(f"Token 无效: {e}")
        return None
    except Exception as e:
        print(f"Token 解析失败: {e}")
        return None

def get_user_id_from_token(token=None):
    """
    解析 Spring Boot 生成的 JWT token，提取用户名并返回用户ID
    如果 token 为空，则从 Flask 请求头中获取
    如果 token 无效或解析失败，返回默认用户 ID 1
    """
    try:
        # 获取用户名
        username = get_username_from_token(token)
        if not username:
            print("使用默认用户ID 1")
            return 1

        # 从数据库中查找用户ID
        from database import Database
        db = Database()
        user = db.get_user_by_username(username)

        if user:
            print(f"找到用户: {user}")
            return user['id']
        else:
            print(f"用户不存在，使用默认用户ID 1")
            return 1

    except Exception as e:
        print(f"获取用户ID失败: {e}，使用默认用户ID 1")
        return 1

def validate_token(token):
    """验证 token 是否有效"""
    try:
        if not token:
            return False

        # 去掉 "Bearer " 前缀（如果存在）
        if token.startswith('Bearer '):
            token = token[7:]

        # 解析 JWT token（不验证签名，只验证格式）
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM], options={"verify_signature": False})

        # 检查是否过期
        exp = payload.get('exp')
        if exp and exp < time.time():
            return False

        return True
    except Exception as e:
        print(f"Token 验证失败: {e}")
        return False
