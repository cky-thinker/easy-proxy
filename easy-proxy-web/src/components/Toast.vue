<template>
  <div v-if="visible" class="fixed top-20 left-1/2 -translate-x-1/2 z-50">
    <div
      :class="[
        'shadow-lg rounded-md px-4 py-3 flex items-center space-x-3',
        type === 'success' ? 'bg-green-50 border border-green-200 text-green-800' :
        type === 'error' ? 'bg-red-50 border border-red-200 text-red-800' :
        'bg-blue-50 border border-blue-200 text-blue-800'
      ]"
    >
      <span class="text-sm">{{ message }}</span>
      <button @click="close" aria-label="关闭" class="text-xs text-gray-500 hover:text-gray-700 cursor-pointer">×</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

interface Props {
  show: boolean
  message: string
  type?: ToastType
  duration?: number
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  duration: 2500
})

const emit = defineEmits(['update:show'])

const visible = ref(props.show)
const message = ref(props.message)
const type = ref(props.type)
let timer: number | null = null

// 启动自动关闭计时器
const startTimer = () => {
  if (timer) {
    window.clearTimeout(timer)
  }
  
  if (props.duration > 0) {
    timer = window.setTimeout(() => {
      visible.value = false
      timer = null
    }, props.duration)
  }
}

// 监听props变化
watch(() => props.show, (newVal) => {
  visible.value = newVal
  if (newVal) {
    message.value = props.message
    type.value = props.type
    startTimer()
  }
}, { immediate: true })

watch(() => props.message, (newVal) => {
  message.value = newVal
})

watch(() => props.type, (newVal) => {
  type.value = newVal
})

// 监听visible变化，同步更新父组件的show属性
watch(visible, (newVal) => {
  emit('update:show', newVal)
})

// 关闭Toast
const close = () => {
  visible.value = false
  if (timer) {
    window.clearTimeout(timer)
    timer = null
  }
}

// 组件挂载时，如果show为true，启动计时器
if (props.show) {
  startTimer()
}
</script>