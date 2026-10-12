<!-- src/views/DeviceMonitor.vue -->
<template>
  <div class="device-monitor">
    <h2>树莓派监控面板</h2>
    <div v-if="!connected" class="status offline">连接中...</div>
    <div v-else class="status online">已连接</div>

    <!-- 显示位置 -->
    <div class="location-info">
      <h3>设备位置</h3>
      <p>{{ locationText || '未知' }}</p>
    </div>

    <div class="sensor-data">
      <h3>最新温湿度</h3>
      <p>温度: {{ temperature }} °C</p>
      <p>湿度: {{ humidity }} %</p>
    </div>

    <div class="controls">
      <button @click="sendCommand('restart')" :disabled="!connected">重启树莓派</button>
      <button @click="sendCommand('shutdown')" :disabled="!connected">关机树莓派</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const ws = ref(null)
const connected = ref(false)
const temperature = ref('--')
const humidity = ref('--')
const locationText = ref('') 
const deviceId = '192.168.153.136'  // 与树莓派ID一致

// 注意：后端 WebSocket 地址根据实际情况修改
// 如果前端和后端在同一台电脑上，可以用 localhost
// 如果前端通过局域网访问后端，则改为后端的局域网 IP
const BACKEND_WS_URL = 'ws://localhost:8080/ws/frontend'

const connect = () => {
  ws.value = new WebSocket(BACKEND_WS_URL)

  ws.value.onopen = () => {
    console.log('前端WebSocket已连接')
    connected.value = true
  }

  ws.value.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.type === 'sensor') {
      temperature.value = data.data.temperature.toFixed(1)
      humidity.value = data.data.humidity.toFixed(1)
      // 显示位置（如果后端转发的数据中包含 location）
      if (data.location) {
        if (typeof data.location === 'string') {
          locationText.value = data.location
        } else if (data.location.lng && data.location.lat) {
          locationText.value = `经度: ${data.location.lng}, 纬度: ${data.location.lat}`
        }
      }
    }
  }

  ws.value.onerror = (err) => {
    console.error('WebSocket错误', err)
    connected.value = false
  }

  ws.value.onclose = () => {
    console.log('WebSocket断开，3秒后重连')
    connected.value = false
    setTimeout(connect, 3000)
  }
}

const sendCommand = (command) => {
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    const msg = JSON.stringify({ deviceId, command })
    ws.value.send(msg)
    alert(`已发送指令: ${command}`)
  } else {
    alert('未连接到后端')
  }
}

onMounted(() => {
  connect()
})

onUnmounted(() => {
  if (ws.value) ws.value.close()
})
</script>

<style scoped>

.location-info {
  margin: 20px 0;
  padding: 15px;
  background: #e8f4f8;
  border-radius: 8px;
  border-left: 4px solid #3498db;
}

.device-monitor {
  padding: 20px;
}
.status {
  padding: 5px 10px;
  border-radius: 5px;
  display: inline-block;
}
.online { background: #2ecc71; color: white; }
.offline { background: #e74c3c; color: white; }
.sensor-data {
  margin: 20px 0;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 8px;
}
.controls button {
  margin-right: 10px;
  padding: 8px 16px;
  cursor: pointer;
}
</style>