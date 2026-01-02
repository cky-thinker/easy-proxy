<template>
  <div class="traffic-chart">
    <div class="chart-header mb-4 flex items-center justify-between">
      <h3 class="text-lg font-semibold text-gray-900">流量趋势</h3>
      <div class="flex space-x-2">
        <button
          v-for="period in periods"
          :key="period.value"
          @click="selectedPeriod = period.value"
          :style="selectedPeriod === period.value ? { backgroundColor: 'var(--el-color-primary)', color: '#fff' } : {}"
          :class="[
            'px-3 py-1 text-sm rounded-md transition-colors',
            selectedPeriod === period.value
              ? 'text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          ]"
        >
          {{ period.label }}
        </button>
      </div>
    </div>

    <div class="chart-container w-full h-64" ref="chartContainer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
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
let chart: echarts.ECharts | null = null

const periods = [
  { label: '今日', value: 'day' as const },
  { label: '本周', value: 'week' as const },
  { label: '本月', value: 'month' as const }
]

const formatBytes = (bytes: number): string => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const renderChart = () => {
  if (!chartContainer.value) return
  if (!chart) {
    chart = echarts.init(chartContainer.value)
  }
  const times = (props.data || []).map(d => new Date(d.time).toLocaleString())
  const upload = (props.data || []).map(d => d.upload)
  const download = (props.data || []).map(d => d.download)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p0 = params[0]
        const p1 = params[1]
        const time = p0?.axisValueLabel || ''
        const up = formatBytes(p0?.data || 0)
        const down = formatBytes(p1?.data || 0)
        return `${time}<br/>上传: ${up}<br/>下载: ${down}`
      }
    },
    legend: { data: ['上传', '下载'] },
    grid: { left: 24, right: 16, top: 32, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: times },
    yAxis: { type: 'value' },
    series: [
      { name: '上传', type: 'line', data: upload, smooth: true, lineStyle: { width: 2 } },
      { name: '下载', type: 'line', data: download, smooth: true, lineStyle: { width: 2 } }
    ]
  }
  chart.setOption(option)
  chart.resize()
}

const emit = defineEmits<{
  periodChange: [period: 'day' | 'week' | 'month']
}>()

watch(selectedPeriod, (newPeriod) => {
  emit('periodChange', newPeriod)
})

onMounted(() => {
  renderChart()
  const resizeObserver = new ResizeObserver(() => {
    if (chart) chart.resize()
  })
  if (chartContainer.value) resizeObserver.observe(chartContainer.value)
})

watch(() => props.data, () => renderChart(), { deep: true })

onBeforeUnmount(() => {
  if (chart) { chart.dispose(); chart = null }
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