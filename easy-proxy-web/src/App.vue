<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
    <div class="flex flex-col min-h-screen bg-gray-50">
        <!-- 顶部导航栏 -->
        <header class="bg-white shadow-sm py-4 px-6">
            <div class="container mx-auto flex justify-between items-center">
                <div class="flex items-center space-x-4">
                    <span class="logo w-8 h-8 bg-blue-500 rounded-md"></span>
                    <span class="title text-xl font-bold text-gray-800">Easy Proxy</span>
                </div>
            </div>
        </header>

        <!-- 主体内容区 -->
        <div class="flex flex-1 container mx-auto mt-6 px-6">
            <!-- 侧边栏导航 -->
            <aside class="w-64 pr-8">
                <div class="bg-white rounded-lg shadow-sm p-4">
                    <div class="menu space-y-2">
                        <div class="item py-2 px-4 rounded-md hover:bg-blue-50 transition-colors">
                            <RouterLink to="/" class="block text-gray-700 hover:text-blue-600 font-medium">Home</RouterLink>
                        </div>
                        <div class="item py-2 px-4 rounded-md hover:bg-blue-50 transition-colors">
                            <RouterLink to="/about" class="block text-gray-700 hover:text-blue-600 font-medium">About</RouterLink>
                        </div>
                        <div class="item py-2 px-4 rounded-md hover:bg-blue-50 transition-colors">
                            <RouterLink to="/proxy" class="block text-gray-700 hover:text-blue-600 font-medium">代理管理</RouterLink>
                        </div>
                        <div class="item py-2 px-4 rounded-md hover:bg-blue-50 transition-colors">
                            <RouterLink to="/tailwind" class="block text-gray-700 hover:text-blue-600 font-medium">Tailwind示例</RouterLink>
                        </div>
                    </div>
                </div>
            </aside>

            <!-- 内容区域 -->
            <div class="flex-1">
                <main class="bg-white rounded-lg shadow-sm p-6 min-h-[500px]">
                    <RouterView />
                </main>
                
                <!-- 页脚 -->
                <footer class="mt-6 py-4 text-center text-gray-600 text-sm">
                    <div>
                        <p>
                            Copyright &copy; 2023 Easy Proxy. All rights reserved.
                        </p>
                    </div>
                </footer>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 可以在这里添加任何不能通过Tailwind实现的自定义样式 */
</style>
