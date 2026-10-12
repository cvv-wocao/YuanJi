<template>
  <div class="device-detail">
    <h2>设备详情</h2>
    <div v-if="deviceInfo" v-loading="loading">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="设备ID">{{ deviceInfo.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ deviceInfo.location || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="温度">{{ deviceInfo.temperature ?? '--' }} °C</el-descriptions-item>
        <el-descriptions-item label="湿度">{{ deviceInfo.humidity ?? '--' }} %</el-descriptions-item>
        <el-descriptions-item label="最后在线">
          {{ deviceInfo.lastSeen ? new Date(deviceInfo.lastSeen).toLocaleString() : '--' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="controls">
        <el-button type="danger" @click="sendCommand('restart')" :loading="cmdLoading">重启树莓派</el-button>
        <el-button type="warning" @click="sendCommand('shutdown')" :loading="cmdLoading">关机树莓派</el-button>
      </div>
    </div>
    <div v-else-if="!loading">未找到设备</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const deviceId = route.params.deviceId
const deviceInfo = ref(null)
const loading = ref(false)
const cmdLoading = ref(false)
let interval = null

const fetchDeviceData = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await axios.get(`/api/admin/devices/${deviceId}`)
    deviceInfo.value = res.data
  } catch (error) {
    console.error('获取设备数据失败', error)
  } finally {
    loading.value = false
  }
}

const sendCommand = async (command) => {
  cmdLoading.value = true
  try {
    await axios.post(`/api/admin/devices/${deviceId}/command`, { command })
    ElMessage.success(`指令 ${command} 已发送`)
  } catch (error) {
    ElMessage.error('发送失败，请确保设备在线')
  } finally {
    cmdLoading.value = false
  }
}

onMounted(() => {
  fetchDeviceData()
  interval = setInterval(fetchDeviceData, 3000) // 每3秒刷新一次数据
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})
</script>

<style scoped>
.device-detail {
  padding: 20px;
}
.controls {
  margin-top: 30px;
}
.controls .el-button {
  margin-right: 15px;
}
</style>