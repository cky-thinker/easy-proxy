<template>
  <span :class="[
    'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
    isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
  ]">
    {{ isActive ? '在线' : '离线' }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ value?: 'online' | 'offline' | string }>()
const isActive = computed(() => props.value === 'online')
</script>