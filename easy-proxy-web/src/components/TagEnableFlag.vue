<template>
  <span :class="[
    'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
    props.value ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
  ]">
    {{ props.value ? '激活' : '禁用' }}
  </span>
</template>

<script setup lang="ts">
const props = defineProps<{ value?: boolean }>()
</script>