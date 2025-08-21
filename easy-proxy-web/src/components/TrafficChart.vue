<template>
  <div class="traffic-chart">
    <div class="chart-header mb-4">
      <h3 class="text-lg font-semibold text-gray-900">流量趋势</h3>
      <div class="flex space-x-2">
        <button
          v-for="period in periods"
          :key="period.value"
          @click="selectedPeriod = period.value"
          :class="[
            'px-3 py-1 text-sm rounded-md transition-colors',
            selectedPeriod === period.value
              ? 'bg-indigo-600 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          ]"
        >
          {{ period.label }}
        </button>
      </div>
    </div>
    
    <div class="chart-container" ref="chartContainer">
      <!-- 简单的SVG图表实现 -->
      <svg 
        :width="chartWidth" 
        :height="chartHeight" 
        class="w-full h-64"
        viewBox="0 0 800 300"
      >
        <!-- 网格线 -->
        <defs>
          <pattern id="grid" width="80" height="30" patternUnits="userSpaceOnUse">
            <path d="M 80 0 L 0 0 0 30" fill="none" stroke="#e5e7eb" stroke-width="1"/>
          </pattern>
        </defs>
        <rect width="100%" height="100%" fill="url(#grid)" />
        
        <!-- 上传数据线 -->
        <polyline
          :points="uploadPoints"
          fill="none"
          stroke="#3b82f6"
          stroke-width="2"
          class="upload-line"
        />
        
        <!-- 下载数据线 -->
        <polyline
          :points="downloadPoints"
          fill="none"
          stroke="#10b981"
          stroke-width="2"
          class="download-line"
        />
        
        <!-- 数据点 -->
        <circle
          v-for="(point, index) in uploadData"
          :key="`upload-${index}`"
          :cx="getX(index)"
          :cy="getY(point.upload)"
          r="3"
          fill="#3b82f6"
          class="cursor-pointer hover:r-4 transition-all"
          @mouseover="showTooltip($event, point, 'upload')"
          @mouseout="hideTooltip"
        />
        
        <circle
          v-for="(point, index) in downloadData"
          :key="`download-${index}`"
          :cx="getX(index)"
          :cy="getY(point.download)"
          r="3"
          fill="#10b981"
          class="cursor-pointer hover:r-4 transition-all"
          @mouseover="showTooltip($event, point, 'download')"
          @mouseout="hideTooltip"
        />
      </svg>
    </div>
    
    <!-- 图例 -->
    <div class="chart-legend mt-4 flex justify-center space-x-6">
      <div class="flex items-center">
        <div class="w-3 h-3 bg-blue-500 rounded-full mr-2"></div>
        <span class="text-sm text-gray-600">上传流量</span>
      </div>
      <div class="flex items-center">
        <div class="w-3 h-3 bg-green-500 rounded-full mr-2"></div>
        <span class="text-sm text-gray-600">下载流量</span>
      </div>
    </div>
    
    <!-- 工具提示 -->
    <div
      v-if="tooltip.show"
      :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
      class="absolute bg-gray-800 text-white px-2 py-1 rounded text-sm pointer-events-none z-10"
    >
      <div>时间: {{ tooltip.time }}</div>
      <div>{{ tooltip.type === 'upload' ? '上传' : '下载' }}: {{ formatBytes(tooltip.value) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { TrafficTrend } from '@/api/types'

interface Props {
  data?: TrafficTrend[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false
})

const selectedPeriod = ref<'day' | 'week' | 'month'>('day')
const chartContainer = ref<HTMLElement | null>(null)
const chartWidth = ref(800)
const chartHeight = ref(300)

const periods = [
  { label: '今日', value: 'day' as const },
  { label: '本周', value: 'week' as const },
  { label: '本月', value: 'month' as const }
]

const tooltip = ref({
  show: false,
  x: 0,
  y: 0,
  time: '',
  value: 0,
  type: 'upload' as 'upload' | 'download'
})

// 模拟数据
const mockData = computed(() => {
  if (props.data.length > 0) return props.data
  
  const data: TrafficTrend[] = []
  const now = new Date()
  
  for (let i = 23; i >= 0; i--) {
    const time = new Date(now.getTime() - i * 60 * 60 * 1000)
    data.push({
      time: time.toISOString(),
      upload: Math.random() * 1000000 + 500000,
      download: Math.random() * 2000000 + 1000000
    })
  }
  
  return data
})

const uploadData = computed(() => mockData.value)
const downloadData = computed(() => mockData.value)

const maxValue = computed(() => {
  const allValues = [...uploadData.value.map(d => d.upload), ...downloadData.value.map(d => d.download)]
  return Math.max(...allValues)
})

const getX = (index: number): number => {
  const padding = 40
  const width = chartWidth.value - padding * 2
  return padding + (index / (uploadData.value.length - 1)) * width
}

const getY = (value: number): number => {
  const padding = 20
  const height = chartHeight.value - padding * 2
  return padding + (1 - value / maxValue.value) * height
}

const uploadPoints = computed(() => {
  return uploadData.value.map((point, index) => {
    const x = getX(index)
    const y = getY(point.upload)
    return `${x},${y}`
  }).join(' ')
})

const downloadPoints = computed(() => {
  return downloadData.value.map((point, index) => {
    const x = getX(index)
    const y = getY(point.download)
    return `${x},${y}`
  }).join(' ')
})

const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const showTooltip = (event: MouseEvent, point: TrafficTrend, type: 'upload' | 'download') => {
  const rect = chartContainer.value?.getBoundingClientRect()
  if (rect) {
    tooltip.value = {
      show: true,
      x: event.clientX - rect.left + 10,
      y: event.clientY - rect.top - 10,
      time: new Date(point.time).toLocaleTimeString(),
      value: type === 'upload' ? point.upload : point.download,
      type
    }
  }
}

const hideTooltip = () => {
  tooltip.value.show = false
}

const emit = defineEmits<{
  periodChange: [period: 'day' | 'week' | 'month']
}>()

watch(selectedPeriod, (newPeriod) => {
  emit('periodChange', newPeriod)
})

onMounted(() => {
  // 响应式调整图表大小
  const resizeObserver = new ResizeObserver(() => {
    if (chartContainer.value) {
      const rect = chartContainer.value.getBoundingClientRect()
      chartWidth.value = rect.width
    }
  })
  
  if (chartContainer.value) {
    resizeObserver.observe(chartContainer.value)
  }
})
</script>

<style scoped>
.traffic-chart {
  position: relative;
}

.upload-line {
  filter: drop-shadow(0 2px 4px rgba(59, 130, 246, 0.3));
}

.download-line {
  filter: drop-shadow(0 2px 4px rgba(16, 185, 129, 0.3));
}

.chart-container {
  background: linear-gradient(to bottom, #f9fafb, #ffffff);
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
</style>