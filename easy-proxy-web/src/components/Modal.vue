<template>
  <div v-if="modelValue" class="fixed inset-0 bg-gray-800/30 overflow-y-auto h-full w-full z-50">
    <div :class="['relative top-20 mx-auto p-5 border border-gray-200 shadow-lg rounded-md bg-white', width]">
      <div class="mt-3">
        <h3 v-if="title" class="text-lg font-medium text-gray-900 mb-4">
          {{ title }}
        </h3>
        <slot></slot>
        <div v-if="showFooter" class="flex justify-end space-x-3 mt-6">
          <button
            v-if="showCancelButton"
            @click="cancel"
            class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 cursor-pointer"
          >
            {{ cancelText }}
          </button>
          <button
            v-if="showConfirmButton"
            @click="confirm"
            class="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700 cursor-pointer"
          >
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: 'w-96'
  },
  showFooter: {
    type: Boolean,
    default: true
  },
  showCancelButton: {
    type: Boolean,
    default: true
  },
  showConfirmButton: {
    type: Boolean,
    default: true
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  confirmText: {
    type: String,
    default: '确定'
  }
})

const emit = defineEmits(['update:modelValue', 'cancel', 'confirm'])

const cancel = () => {
  emit('cancel')
  emit('update:modelValue', false)
}

const confirm = () => {
  emit('confirm')
  emit('update:modelValue', false)
}
</script>