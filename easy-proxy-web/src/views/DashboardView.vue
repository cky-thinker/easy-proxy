<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">总览</h1>
      <p class="text-gray-600 mt-1">代理服务器运行状态和统计信息</p>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- 在线客户端 -->
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="p-2 bg-green-100 rounded-lg">
            <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">在线客户端</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.onlineClients }}</p>
          </div>
        </div>
      </div>

      <!-- 离线客户端 -->
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="p-2 bg-red-100 rounded-lg">
            <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">离线客户端</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.offlineClients }}</p>
          </div>
        </div>
      </div>

      <!-- 总流量 -->
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="p-2 bg-blue-100 rounded-lg">
            <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"></path>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">总流量</p>
            <p class="text-2xl font-semibold text-gray-900">{{ formatBytes(stats.totalTraffic) }}</p>
          </div>
        </div>
      </div>

      <!-- 活跃连接 -->
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="p-2 bg-purple-100 rounded-lg">
            <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">活跃连接</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.activeConnections }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 流量排行 -->
      <div class="bg-white rounded-lg shadow">
        <div class="p-6 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-900">流量排行</h2>
            <select v-model="trafficPeriod" class="text-sm border border-gray-300 rounded-md px-3 py-1">
              <option value="day">按天</option>
              <option value="week">按周</option>
              <option value="month">按月</option>
            </select>
          </div>
        </div>
        <div class="p-6">
          <div class="space-y-4">
            <div v-for="(item, index) in trafficRanking" :key="index" class="flex items-center justify-between">
              <div class="flex items-center">
                <div class="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center mr-3">
                  <span class="text-sm font-medium text-indigo-600">{{ index + 1 }}</span>
                </div>
                <div>
                  <p class="text-sm font-medium text-gray-900">{{ item.name }}</p>
                  <p class="text-xs text-gray-500">{{ item.ip }}</p>
                </div>
              </div>
              <div class="text-right">
                <p class="text-sm font-medium text-gray-900">{{ formatBytes(item.traffic) }}</p>
                <p class="text-xs text-gray-500">{{ item.connections }} 连接</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时流量趋势 -->
      <div class="bg-white rounded-lg shadow">
        <div class="p-6">
          <TrafficChart 
            :data="trafficTrendData" 
            :loading="loading.trafficTrend"
            @period-change="handlePeriodChange"
          />
        </div>
      </div>
    </div>

    <!-- 最近活动 -->
    <div class="mt-6 bg-white rounded-lg shadow">
      <div class="p-6 border-b border-gray-200">
        <h2 class="text-lg font-semibold text-gray-900">最近活动</h2>
      </div>
      <div class="p-6">
        <div class="space-y-4">
          <div v-for="(activity, index) in recentActivities" :key="index" class="flex items-start">
            <div class="flex-shrink-0">
              <div class="w-2 h-2 bg-blue-400 rounded-full mt-2"></div>
            </div>
            <div class="ml-4">
              <p class="text-sm text-gray-900">{{ activity.message }}</p>
              <p class="text-xs text-gray-500 mt-1">{{ activity.time }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TrafficChart from '@/components/TrafficChart.vue'
import type { DashboardStats, TrafficRanking, RecentActivity, TrafficTrend } from '@/api/types'

// 响应式数据
const stats = ref({
  onlineClients: 0,
  offlineClients: 0,
  totalTraffic: 0,
  activeConnections: 0
})

const trafficPeriod = ref('day')
const trendPeriod = ref('day')

const trafficTrendData = ref<TrafficTrend[]>([])
const loading = ref({
  trafficTrend: false
})

const trafficRanking = ref([
  { name: '客户端-001', ip: '192.168.1.100', traffic: 1024 * 1024 * 500, connections: 15 },
  { name: '客户端-002', ip: '192.168.1.101', traffic: 1024 * 1024 * 320, connections: 8 },
  { name: '客户端-003', ip: '192.168.1.102', traffic: 1024 * 1024 * 280, connections: 12 },
  { name: '客户端-004', ip: '192.168.1.103', traffic: 1024 * 1024 * 150, connections: 5 },
  { name: '客户端-005', ip: '192.168.1.104', traffic: 1024 * 1024 * 90, connections: 3 }
])

const recentActivities = ref([
  { message: '客户端-001 上线', time: '2分钟前' },
  { message: '客户端-003 流量异常', time: '5分钟前' },
  { message: '新增代理规则：端口8080', time: '10分钟前' },
  { message: '客户端-002 下线', time: '15分钟前' },
  { message: '系统启动完成', time: '1小时前' }
])

// 工具函数
const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 处理周期变化
const handlePeriodChange = (period: string) => {
  trendPeriod.value = period
  loadTrafficTrendData()
}

// 加载流量趋势数据
const loadTrafficTrendData = async () => {
  loading.value.trafficTrend = true
  try {
    // 模拟流量趋势数据
    trafficTrendData.value = []
  } catch (error) {
    console.error('加载流量趋势数据失败:', error)
  } finally {
    loading.value.trafficTrend = false
  }
}

// 加载数据
const loadDashboardData = async () => {
  try {
    // 模拟数据加载
    stats.value = {
      onlineClients: 12,
      offlineClients: 3,
      totalTraffic: 1024 * 1024 * 1024 * 2.5, // 2.5GB
      activeConnections: 45
    }
    await loadTrafficTrendData()
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>