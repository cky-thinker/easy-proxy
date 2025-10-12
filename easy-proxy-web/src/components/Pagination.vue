<template>
  <div class="w-full mt-4 flex items-center justify-end">
    <div class="flex items-center space-x-2" v-if="total > 0 && totalPage > 1">
      <template v-for="item in displayItems" :key="itemKey(item)">
        <button
          v-if="item.type === 'page'"
          class="px-3 py-1 rounded border text-sm"
          :class="item.value - 1 === currentPage ? 'bg-blue-600 text-white border-blue-600' : 'bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200'"
          :disabled="loading"
          @click="$emit('change', item.value - 1)"
        >{{ item.value }}</button>
        <span v-else class="px-2 text-gray-500">...</span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  currentPage: number
  pageSize: number
  total: number
  totalPage: number
  loading?: boolean
}>()

type DisplayItem = { type: 'page', value: number } | { type: 'ellipsis', value: null, pos: 'left' | 'right' }

const displayItems = computed<DisplayItem[]>(() => {
  const tp = props.totalPage
  const cp = props.currentPage + 1 // 转为 1 基页码
  const items: DisplayItem[] = []

  if (tp <= 7) {
    for (let p = 1; p <= tp; p++) items.push({ type: 'page', value: p })
    return items
  }

  const start = Math.max(1, cp - 2)
  const end = Math.min(tp, cp + 2)

  // 首页
  items.push({ type: 'page', value: 1 })

  // 左侧省略号
  if (start > 2) items.push({ type: 'ellipsis', value: null, pos: 'left' })

  // 中间窗口（排除首尾）
  for (let p = start; p <= end; p++) {
    if (p !== 1 && p !== tp) items.push({ type: 'page', value: p })
  }

  // 右侧省略号
  if (end < tp - 1) items.push({ type: 'ellipsis', value: null, pos: 'right' })

  // 尾页
  items.push({ type: 'page', value: tp })

  return items
})

const itemKey = (item: DisplayItem) => {
  return item.type === 'page' ? `p-${item.value}` : `e-${item.pos}`
}
</script>

<style scoped></style>