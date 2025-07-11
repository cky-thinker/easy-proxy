<script setup lang="ts">
import TailwindExample from '../components/TailwindExample.vue';
</script>

<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-6">Tailwind CSS 测试页面</h1>
    
    <div class="bg-white shadow rounded-lg p-6 mb-8">
      <h2 class="text-xl font-semibold mb-4">Tailwind CSS 简介</h2>
      <p class="mb-4">
        Tailwind CSS 是一个功能类优先的 CSS 框架，它集成了诸如 flex, pt-4, text-center 和 rotate-90 这样的的类，它们能直接在脚本标记语言中组合起来，构建出任何设计。
      </p>
      <ul class="flex flex-col gap-2 list-disc list-inside mb-4">
        <li>无需离开HTML即可快速构建自定义设计</li>
        <li>使用功能类构建完全响应式的界面</li>
        <li>使用移动优先的策略构建复杂的响应式布局</li>
        <li>通过配置文件轻松定制设计系统</li>
      </ul>
    </div>
    
    <TailwindExample />
  </div>
</template>