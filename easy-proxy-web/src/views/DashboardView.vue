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
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/>
              <circle cx="12" cy="12" r="9" stroke-width="2"></circle>
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
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 6l12 12M18 6l-12 12"/>
              <circle cx="12" cy="12" r="9" stroke-width="2"></circle>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">离线客户端</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.offlineClients }}</p>
          </div>
        </div>
      </div>

      <!-- 总上行流量 -->
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="p-2 bg-blue-100 rounded-lg">
            <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v12M7 9l5-5 5 5"/>
              <rect x="4" y="16" width="16" height="4" rx="1" stroke-width="2"></rect>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">总上行流量</p>
            <p class="text-2xl font-semibold text-gray-900">{{ formatBytes(stats.totalUpload) }}</p>
          </div>
        </div>
      </div>

      <!-- 总下行流量 -->
      <div class="bg-white rounded-lg shadow p-6">
        <div class="flex items-center">
          <div class="p-2 bg-green-100 rounded-lg">
            <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 20V8M17 15l-5 5-5-5"/>
              <rect x="4" y="4" width="16" height="4" rx="1" stroke-width="2"></rect>
            </svg>
          </div>
          <div class="ml-4">
            <p class="text-sm font-medium text-gray-600">总下行流量</p>
            <p class="text-2xl font-semibold text-gray-900">{{ formatBytes(stats.totalDownload) }}</p>
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
            <div class="flex space-x-2">
              <button
                v-for="p in periods"
                :key="p.value"
                @click="changeRankingPeriod(p.value)"
                :style="trafficPeriod === p.value ? { backgroundColor: 'var(--el-color-primary)', color: '#fff' } : {}"
                :class="[
                  'px-3 py-1 text-sm rounded-md transition-colors',
                  trafficPeriod === p.value
                    ? 'text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                ]"
              >
                {{ p.label }}
              </button>
            </div>
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
import { getDashboardStats, getTrafficRanking, getTrafficTrend, getRecentActivities } from '@/api/dashboard'

// 响应式数据
const stats = ref<DashboardStats>({
  onlineClients: 0,
  offlineClients: 0,
  totalUpload: 0,
  totalDownload: 0
})

const trafficPeriod = ref('day')
const trendPeriod = ref('day')
const periods = [
  { label: '按天', value: 'day' as const },
  { label: '按周', value: 'week' as const },
  { label: '按月', value: 'month' as const }
]

const trafficTrendData = ref<TrafficTrend[]>([])
const loading = ref({
  trafficTrend: false
})

const trafficRanking = ref<TrafficRanking[]>([])

const recentActivities = ref<RecentActivity[]>([])

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

const changeRankingPeriod = async (period: 'day'|'week'|'month') => {
  trafficPeriod.value = period
  await loadTrafficRanking()
}

// 加载流量趋势数据
const loadTrafficTrendData = async () => {
  loading.value.trafficTrend = true
  try {
    trafficTrendData.value = await getTrafficTrend(trendPeriod.value as 'day'|'week'|'month')
  } catch (error) {
    console.error('加载流量趋势数据失败:', error)
  } finally {
    loading.value.trafficTrend = false
  }
}

const loadTrafficRanking = async () => {
  trafficRanking.value = await getTrafficRanking(trafficPeriod.value as 'day'|'week'|'month')
}

// 加载数据
const loadDashboardData = async () => {
  try {
    stats.value = await getDashboardStats()
    await loadTrafficRanking()
    recentActivities.value = await getRecentActivities()
    await loadTrafficTrendData()
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>
