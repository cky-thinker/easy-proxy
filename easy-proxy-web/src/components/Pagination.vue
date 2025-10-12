<template>
  <div class="mt-4 p-4 flex items-center justify-end space-x-3">
    <span class="text-sm text-gray-700">共 {{ total }} 条</span>
    <div class="flex items-center space-x-2" v-if="total > 0 && totalPage > 1">
      <button v-for="page in pages" :key="page" class="px-3 py-1 rounded border text-sm"
        :class="page - 1 === currentPage ? 'bg-blue-600 text-white border-blue-600' : 'bg-gray-100 text-gray-700 border-gray-300 hover:bg-gray-200'"
        :disabled="loading" @click="$emit('change', page - 1)">{{ page }}</button>
    </div>
    <div v-else class="text-sm text-gray-500">暂无数据</div>
  </div>

</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  currentPage: number
  pageSize: number
  total: number
  loading?: boolean
}>()

const totalPage = computed(() => {
  if (!props.pageSize) return 0
  return Math.ceil(props.total / props.pageSize)
})

const pages = computed(() => {
  const tp = totalPage.value
  if (tp <= 1) return []
  // 简单页码列表 1..tp，视图较小项目可直接展示
  return Array.from({ length: tp }, (_, i) => i + 1)
})
</script>

<style scoped></style>