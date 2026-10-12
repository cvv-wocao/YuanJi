import sqlite3
import os
import json
from datetime import datetime

class Database:
    def __init__(self, db_path='running.db'):
        self.db_path = db_path
        self.conn = None
        self.cursor = None
        self.init_db()
    
    def init_db(self):
        """初始化数据库表结构"""
        self.conn = sqlite3.connect(self.db_path, check_same_thread=False)
        self.conn.execute("PRAGMA journal_mode=WAL;")
        self.cursor = self.conn.cursor()
        
        # 用户表
        self.cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE NOT NULL,
            created_at TEXT
        )''')
        
        # 运动记录表
        self.cursor.execute('''
        CREATE TABLE IF NOT EXISTS exercise_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER DEFAULT 1,
            exercise_type TEXT NOT NULL,
            duration INTEGER,
            calories INTEGER,
            video_url TEXT,
            created_at TEXT,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )''')
        
        # 分析报告表
        self.cursor.execute('''
        CREATE TABLE IF NOT EXISTS analysis_reports (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            record_id INTEGER NOT NULL,
            score REAL NOT NULL,
            count INTEGER NOT NULL,
            feedback TEXT,
            knee_angle REAL,
            hip_angle REAL,
            trunk_angle REAL,
            overall_angle REAL,
            ai_analysis TEXT,
            created_at TEXT,
            FOREIGN KEY (record_id) REFERENCES exercise_records(id)
        )''')
        
        # 运动建议表
        self.cursor.execute('''
        CREATE TABLE IF NOT EXISTS exercise_suggestions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            report_id INTEGER NOT NULL,
            suggestion_type TEXT NOT NULL,
            content TEXT NOT NULL,
            priority INTEGER DEFAULT 1,
            created_at TEXT,
            FOREIGN KEY (report_id) REFERENCES analysis_reports(id)
        )''')
        
        self.cursor.execute('''
        INSERT OR IGNORE INTO users (username, created_at) VALUES ('default', ?)
        ''', (self.get_current_time(),))
        
        self.conn.commit()
    
    def get_current_time(self):
        """获取当前本地时间的 ISO 8601 格式字符串"""
        return datetime.now().isoformat()
    
    def ensure_user_exists(self, user_id, username=None):
        """确保用户存在于数据库中，如果不存在则创建"""
        if user_id is None:
            raise ValueError("user_id cannot be None")
        
        self.cursor.execute("SELECT id FROM users WHERE id = ?", (user_id,))
        if self.cursor.fetchone():
            return True
        
        name = username if username else f"user_{user_id}"
        self.cursor.execute(
            "INSERT INTO users (id, username, created_at) VALUES (?, ?, ?)",
            (user_id, name, self.get_current_time())
        )
        self.conn.commit()
        print(f"✅ 自动创建用户: id={user_id}, username={name}")
        return True
    
    def close(self):
        """关闭数据库连接"""
        if self.conn:
            self.conn.close()

    def get_user_by_username(self, username):
        """根据用户名查找用户，如果不存在则自动创建"""
        try:
            self.cursor.execute('SELECT id, username FROM users WHERE username = ?', (username,))
            row = self.cursor.fetchone()
            if row:
                return {'id': row[0], 'username': row[1]}

            self.cursor.execute('INSERT INTO users (username, created_at) VALUES (?, ?)', 
                              (username, self.get_current_time()))
            self.conn.commit()
            user_id = self.cursor.lastrowid
            print(f"✅ 自动创建用户: id={user_id}, username={username}")
            return {'id': user_id, 'username': username}
        except Exception as e:
            print(f"获取用户失败: {e}")
            return None
    
    def save_exercise_record(self, user_id, exercise_type, duration=None, calories=None, video_url=None):
        """保存运动记录"""
        self.ensure_user_exists(user_id)
        
        current_time = self.get_current_time()
        
        self.cursor.execute('''
        INSERT INTO exercise_records (user_id, exercise_type, duration, calories, video_url, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        ''', (user_id, exercise_type, duration, calories, video_url, current_time))
        self.conn.commit()
        return self.cursor.lastrowid
    
    def save_analysis_report(self, record_id, score, count, feedback, 
                           knee_angle=None, hip_angle=None, trunk_angle=None, overall_angle=None, ai_analysis=None):
        """保存分析报告"""
        feedback_json = json.dumps(feedback) if feedback else '[]'
        
        current_time = self.get_current_time()
        
        self.cursor.execute('''
        INSERT INTO analysis_reports (record_id, score, count, feedback, 
                                   knee_angle, hip_angle, trunk_angle, overall_angle, ai_analysis, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (record_id, score, count, feedback_json, 
             knee_angle, hip_angle, trunk_angle, overall_angle, ai_analysis, current_time))
        self.conn.commit()
        return self.cursor.lastrowid
    
    def save_suggestion(self, report_id, suggestion_type, content, priority=1):
        """保存运动建议"""
        self.cursor.execute('''
        INSERT INTO exercise_suggestions (report_id, suggestion_type, content, priority, created_at)
        VALUES (?, ?, ?, ?, ?)
        ''', (report_id, suggestion_type, content, priority, self.get_current_time()))
        self.conn.commit()
    
    def get_recent_records(self, user_id, limit=10):
        """获取最近的运动记录"""
        self.cursor.execute('''
        SELECT r.id, r.exercise_type, r.created_at, r.duration, r.calories,
               a.score, a.count, a.feedback, a.ai_analysis
        FROM exercise_records r
        LEFT JOIN analysis_reports a ON r.id = a.record_id
        WHERE r.user_id = ?
        ORDER BY r.created_at DESC
        LIMIT ?
        ''', (user_id, limit))
        
        records = []
        for row in self.cursor.fetchall():
            record = {
                'id': row[0],
                'exercise_type': row[1],
                'created_at': row[2],
                'duration': row[3],
                'calories': row[4],
                'score': row[5],
                'count': row[6],
                'feedback': json.loads(row[7]) if row[7] else [],
                'ai_analysis': row[8] if row[8] else ''
            }
            records.append(record)
        return records

    def get_all_records_for_analysis(self, user_id):
        """获取所有历史记录，用于习惯分析"""
        try:
            cursor = self.conn.cursor()
            cursor.execute("""
                SELECT r.id, r.exercise_type, r.created_at,
                       a.score, a.count, a.feedback,
                       a.knee_angle, a.hip_angle, a.trunk_angle, a.overall_angle, a.ai_analysis
                FROM exercise_records r
                JOIN analysis_reports a ON r.id = a.record_id
                WHERE r.user_id = ?
                ORDER BY r.created_at DESC
            """, (user_id,))
            rows = cursor.fetchall()
            records = []
            for row in rows:
                record = {
                    'id': row[0],
                    'exercise_type': row[1],
                    'created_at': row[2],
                    'score': row[3],
                    'count': row[4],
                    'feedback': json.loads(row[5]) if row[5] else [],
                    'knee_angle': row[6],
                    'hip_angle': row[7],
                    'trunk_angle': row[8],
                    'overall_angle': row[9],
                    'ai_analysis': row[10]
                }
                records.append(record)
            cursor.close()
            return records
        except Exception as e:
            print(f"❌ 获取分析用记录失败: {e}")
            return []
    
    def get_recent_records_by_username(self, username, limit=10):
        """根据用户名获取最近的运动记录"""
        user = self.get_user_by_username(username)
        if not user:
            return []
        return self.get_recent_records(user['id'], limit)
    
    def get_all_records_for_analysis_by_username(self, username):
        """根据用户名获取所有历史记录，用于习惯分析"""
        user = self.get_user_by_username(username)
        if not user:
            return []
        return self.get_all_records_for_analysis(user['id'])

    def get_report_by_id(self, report_id):
        """根据报告ID获取详细报告"""
        self.cursor.execute('''
        SELECT a.*, r.exercise_type, r.created_at
        FROM analysis_reports a
        LEFT JOIN exercise_records r ON a.record_id = r.id
        WHERE a.id = ?
        ''', (report_id,))
        
        row = self.cursor.fetchone()
        if not row:
            return None
        
        report = {
            'id': row[0],
            'record_id': row[1],
            'score': row[2],
            'count': row[3],
            'feedback': json.loads(row[4]) if row[4] else [],
            'knee_angle': row[5],
            'hip_angle': row[6],
            'trunk_angle': row[7],
            'overall_angle': row[8],
            'ai_analysis': row[9],
            'created_at': row[10],
            'exercise_type': row[11],
            'record_created_at': row[12]
        }
        
        self.cursor.execute('''
        SELECT id, suggestion_type, content, priority
        FROM exercise_suggestions
        WHERE report_id = ?
        ORDER BY priority DESC
        ''', (report_id,))
        
        suggestions = []
        for s_row in self.cursor.fetchall():
            suggestion = {
                'id': s_row[0],
                'type': s_row[1],
                'content': s_row[2],
                'priority': s_row[3]
            }
            suggestions.append(suggestion)
        
        report['suggestions'] = suggestions
        return report

def test_db():
    db = Database()
    print("数据库初始化成功")
    
    record_id = db.save_exercise_record(1, 'running', 60, 100, 'video.mp4')
    print(f"保存运动记录成功，ID: {record_id}")
    
    report_id = db.save_analysis_report(
        record_id, 85.5, 10, ["姿势良好", "节奏稳定"],
        120, 90, 85, 110
    )
    print(f"保存分析报告成功，ID: {report_id}")
    
    db.save_suggestion(report_id, "姿势", "保持膝盖微屈，减少关节压力")
    db.save_suggestion(report_id, "呼吸", "保持均匀呼吸，提高氧气摄入")
    print("保存建议成功")
    
    records = db.get_recent_records(1, 5)
    print("最近的运动记录:")
    for record in records:
        print(record)
    
    report = db.get_report_by_id(report_id)
    print("详细报告:")
    print(report)
    
    db.close()
    print("测试完成")

if __name__ == '__main__':
    test_db()