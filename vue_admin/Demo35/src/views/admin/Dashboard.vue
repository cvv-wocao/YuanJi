<!-- <template>
  <div class="dashboard">
    <h1>数据概览</h1>
    
    <div class="stats-cards">
      <div class="stat-card blue">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <p class="stat-label">总用户数</p>
          <p class="stat-value">{{ stats.userCount }}</p>
        </div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon">🔐</div>
        <div class="stat-info">
          <p class="stat-label">管理员数量</p>
          <p class="stat-value">{{ stats.adminCount }}</p>
        </div>
      </div>
      <div class="stat-card orange">
        <div class="stat-icon">📅</div>
        <div class="stat-info">
          <p class="stat-label">今日新增</p>
          <p class="stat-value">{{ stats.todayNew }}</p>
        </div>
      </div>
      <div class="stat-card purple">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <p class="stat-label">系统运行天数</p>
          <p class="stat-value">{{ stats.runningDays }}</p>
        </div>
      </div>
    </div>

    <div class="dashboard-sections">
      <div class="section">
        <h3>最近注册用户</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>电话</th>
              <th>注册时间</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in recentUsers" :key="user.id">
              <td>{{ user.name }}</td>
              <td>{{ user.number }}</td>
              <td>{{ user.createTime || '未知' }}</td>
              <td><span class="status active">正常</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="section">
        <h3>系统公告</h3>
        <div class="notice-list">
          <div class="notice-item" v-for="(notice, index) in notices" :key="index">
            <span class="notice-dot"></span>
            <span class="notice-text">{{ notice }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAllUsers } from '@/api/admin';

const stats = ref({
  userCount: 0,
  adminCount: 1,
  todayNew: 0,
  runningDays: 1
});

const recentUsers = ref([]);

const notices = [
  '系统运行正常',
  '请定期备份数据库',
  '注意保护用户隐私数据'
];

onMounted(async () => {
  try {
    const result = await getAllUsers();
    if (result.success && result.data) {
      const users = result.data;
      stats.value.userCount = users.length;
      recentUsers.value = users.slice(-5).reverse();
    }
  } catch (error) {
    console.error('获取数据失败:', error);
  }
});
</script>

<style scoped>
.dashboard h1 {
  margin-bottom: 24px;
  color: #333;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-card.blue { border-left: 4px solid #1890ff; }
.stat-card.green { border-left: 4px solid #52c41a; }
.stat-card.orange { border-left: 4px solid #fa8c16; }
.stat-card.purple { border-left: 4px solid #722ed1; }

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-label {
  color: #666;
  font-size: 14px;
  margin: 0 0 8px 0;
}

.stat-value {
  color: #333;
  font-size: 28px;
  font-weight: bold;
  margin: 0;
}

.dashboard-sections {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 16px;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  color: #666;
  font-weight: 500;
  background: #fafafa;
}

.status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status.active {
  background: #f6ffed;
  color: #52c41a;
}

.notice-list {
  padding: 0;
}

.notice-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-dot {
  width: 6px;
  height: 6px;
  background: #1890ff;
  border-radius: 50%;
  margin-right: 12px;
}

.notice-text {
  color: #666;
  font-size: 14px;
}
</style> -->