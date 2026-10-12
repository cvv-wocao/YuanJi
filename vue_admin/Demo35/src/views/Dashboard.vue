<!-- src/views/Dashboard.vue -->
<template>
  <div class="dashboard">
    <h2>实时数据变更大屏</h2>
    <div class="status" :class="{ connected: isConnected }">
      状态: {{ isConnected ? '已连接' : '未连接' }}
    </div>
    <div class="event-list">
      <h3>最新事件</h3>
      <ul>
        <li v-for="(event, index) in events" :key="index">
          <strong>{{ event.eventType }}</strong> - 用户: {{ event.user?.name }} ({{ event.user?.number }}) 
          <strong>{{ event.eventType }}</strong> - 帖子：{{ event.post?.content }} ({{ event.post?.userId }})
          <strong>{{ event.eventType }}</strong> - 商品：{{ event.product?.content }} ({{ event.product?.userId }})
          <strong>{{ event.eventType }}</strong> - 订单：{{ event.order?.content }} ({{ event.order?.userId }})
          <strong>{{ event.eventType }}</strong> - 商品分类：{{ event.category?.content }} ({{ event.category?.userId }})
          <span class="time">{{ formatTime(event.timestamp) }}</span>
        </li>
        <li v-if="events.length === 0">暂无事件</li>
      </ul>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'

export default {
  name: 'Dashboard',
  setup() {
    const ws = ref(null)
    const isConnected = ref(false)
    const events = ref([])

    const formatTime = (timestamp) => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleTimeString()
    }

    const connectWebSocket = () => {
      // 后端 WebSocket 地址，根据实际情况修改 IP 和端口
      const wsUrl = 'ws://localhost:8080/ws/dashboard'
      ws.value = new WebSocket(wsUrl)

      ws.value.onopen = () => {
        console.log('WebSocket 已连接')
        isConnected.value = true
      }

      ws.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('收到消息:', data)
          events.value.unshift(data)
          if (events.value.length > 20) events.value.pop()
        } catch (err) {
          console.error('解析消息失败', err)
        }
      }

      ws.value.onerror = (err) => {
        console.error('WebSocket 错误', err)
        isConnected.value = false
      }

      ws.value.onclose = () => {
        console.log('WebSocket 断开，3秒后重连...')
        isConnected.value = false
        setTimeout(() => {
          connectWebSocket()
        }, 3000)
      }
    }

    onMounted(() => {
      connectWebSocket()
    })

    onUnmounted(() => {
      if (ws.value) {
        ws.value.close()
      }
    })

    return {
      isConnected,
      events,
      formatTime
    }
  }
}
</script>

<style scoped>
.dashboard {
  font-family: 'Arial', sans-serif;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}
h2 {
  color: #2c3e50;
  margin-bottom: 20px;
}
.status {
  padding: 8px 16px;
  background: #e74c3c;
  color: white;
  display: inline-block;
  border-radius: 20px;
  margin-bottom: 20px;
}
.status.connected {
  background: #2ecc71;
}
.event-list {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 15px;
}
.event-list h3 {
  margin-top: 0;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}
ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
li {
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}
li strong {
  color: #3498db;
}
.time {
  float: right;
  color: #999;
  font-size: 12px;
}
</style>