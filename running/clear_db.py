import sqlite3

# 连接到数据库
conn = sqlite3.connect('running.db')
cursor = conn.cursor()

# 获取所有表名
cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
tables = cursor.fetchall()

# 对每个表执行删除操作
for table in tables:
    table_name = table[0]
    if table_name != 'sqlite_sequence':  # 跳过系统表
        print(f"清空表: {table_name}")
        cursor.execute(f"DELETE FROM {table_name};")

# 重置自增计数器
cursor.execute("DELETE FROM sqlite_sequence;")

# 提交更改
conn.commit()

# 关闭连接
conn.close()

print("数据库已清空")