<template>
  <div class="device-list">
    <h2>设备管理</h2>
    <el-table :data="devices" stripe v-loading="loading">
      <el-table-column prop="deviceId" label="设备ID" width="180"></el-table-column>
      <el-table-column prop="location" label="位置"></el-table-column>
      <el-table-column prop="temperature" label="温度(°C)" width="120">
        <template #default="{ row }">
          {{ row.temperature ?? '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="humidity" label="湿度(%)" width="120">
        <template #default="{ row }">
          {{ row.humidity ?? '--' }}
        </template>
      </el-table-column>
      <el-table-column prop="lastSeen" label="最后在线" width="180">
        <template #default="{ row }">
          {{ row.lastSeen ? new Date(row.lastSeen).toLocaleString() : '--' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="viewDetail(row.deviceId)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const devices = ref([])
const loading = ref(false)

const fetchDevices = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/admin/devices')
    devices.value = res.data
  } catch (error) {
    console.error('获取设备列表失败', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (deviceId) => {
  router.push(`/admin/devices/${deviceId}`)
}

onMounted(() => {
  fetchDevices()
  // 每10秒刷新列表
  setInterval(fetchDevices, 10000)
})
</script>

<style scoped>
.device-list {
  padding: 20px;
}
</style>