<script setup lang="ts">
import { ref } from 'vue';

const showAlert = ref(false);
const toggleAlert = () => {
  showAlert.value = !showAlert.value;
};

// 在Vue 3的<script setup>中，所有顶级变量和函数都会自动暴露给模板
// 不需要额外的return语句
</script>

<template>
  <div class="bg-white shadow rounded-lg p-6 mb-8">
    <h2 class="text-xl font-semibold mb-4">Tailwind CSS 示例</h2>
    
    <!-- 卡片示例 -->
    <div class="mb-8">
      <h3 class="text-lg font-medium mb-3">卡片布局</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="bg-white border border-gray-200 rounded-lg shadow hover:shadow-lg transition-shadow duration-300 overflow-hidden">
          <div class="p-4">
            <h4 class="text-lg font-semibold mb-2">基础卡片</h4>
            <p class="text-gray-600">这是一个基础的卡片示例，展示了边框、圆角和阴影效果。</p>
          </div>
          <div class="bg-gray-50 px-4 py-3 border-t border-gray-200">
            <button class="text-blue-600 hover:text-blue-800 font-medium">查看详情</button>
          </div>
        </div>
        
        <div class="bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg shadow text-white overflow-hidden">
          <div class="p-4">
            <h4 class="text-lg font-semibold mb-2">渐变背景</h4>
            <p>Tailwind CSS 提供了丰富的渐变背景选项，可以创建出美观的视觉效果。</p>
          </div>
          <div class="bg-black bg-opacity-20 px-4 py-3">
            <button class="text-white hover:text-gray-200 font-medium">了解更多</button>
          </div>
        </div>
        
        <div class="bg-white border border-gray-200 rounded-lg shadow overflow-hidden">
          <div class="bg-blue-500 text-white p-4">
            <h4 class="text-lg font-semibold">头部强调</h4>
          </div>
          <div class="p-4">
            <p class="text-gray-600">这个卡片使用了彩色的头部区域来强调重要信息。</p>
          </div>
          <div class="bg-gray-50 px-4 py-3 border-t border-gray-200">
            <button class="text-blue-600 hover:text-blue-800 font-medium">操作按钮</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 提示信息 -->
    <div class="mb-8">
      <h3 class="text-lg font-medium mb-3">提示信息</h3>
      <div class="flex flex-col gap-4">
        <div class="bg-blue-50 border-l-4 border-blue-500 text-blue-700 p-4" role="alert">
          <p class="font-bold">信息提示</p>
          <p>这是一个信息提示框，用于展示一般性的提示信息。</p>
        </div>
        
        <div class="bg-green-50 border-l-4 border-green-500 text-green-700 p-4" role="alert">
          <p class="font-bold">成功提示</p>
          <p>这是一个成功提示框，用于展示操作成功的信息。</p>
        </div>
        
        <div class="bg-yellow-50 border-l-4 border-yellow-500 text-yellow-700 p-4" role="alert">
          <p class="font-bold">警告提示</p>
          <p>这是一个警告提示框，用于展示需要注意的信息。</p>
        </div>
        
        <div class="bg-red-50 border-l-4 border-red-500 text-red-700 p-4" role="alert">
          <p class="font-bold">错误提示</p>
          <p>这是一个错误提示框，用于展示错误信息。</p>
        </div>
      </div>
    </div>
    
    <!-- 交互效果 -->
    <div class="mb-8">
      <h3 class="text-lg font-medium mb-3">交互效果</h3>
      <div class="flex flex-wrap gap-4 items-center">
        <button @click="toggleAlert" class="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          {{ showAlert ? '隐藏提示' : '显示提示' }}
        </button>
        
        <div v-if="showAlert" class="bg-blue-100 text-blue-800 px-4 py-3 rounded-lg flex items-center">
          <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"></path>
          </svg>
          <span>这是一个可以通过按钮控制显示/隐藏的提示信息。</span>
        </div>
      </div>
    </div>
    
    <!-- 按钮样式 -->
    <div>
      <h3 class="text-lg font-medium mb-3">按钮样式</h3>
      <div class="flex flex-wrap gap-4">
        <button class="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          主要按钮
        </button>
        
        <button class="bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          次要按钮
        </button>
        
        <button class="bg-green-500 hover:bg-green-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          成功按钮
        </button>
        
        <button class="bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          危险按钮
        </button>
        
        <button class="bg-yellow-500 hover:bg-yellow-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          警告按钮
        </button>
        
        <button class="border border-blue-500 text-blue-500 hover:bg-blue-50 font-semibold py-2 px-4 rounded-lg transition-colors duration-300">
          轮廓按钮
        </button>
        
        <button class="bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-4 rounded-full transition-colors duration-300">
          圆形按钮
        </button>
        
        <button disabled class="bg-gray-300 text-gray-500 font-semibold py-2 px-4 rounded-lg cursor-not-allowed">
          禁用按钮
        </button>
      </div>
    </div>
  </div>
</template>