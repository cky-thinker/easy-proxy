<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
    <div class="flex flex-col min-h-screen bg-gray-50">
        <!-- 顶部导航栏 -->
        <header class="bg-white shadow-sm py-4 px-6">
            <div class="container gap-6 flex justify-start items-center">
                <div class="flex items-center space-x-4">
                    <span class="logo w-8 h-8 bg-blue-500 rounded-md"></span>
                    <span class="title text-xl font-bold text-gray-800">Easy Proxy</span>
                </div>
                
                <!-- 导航菜单 -->
                <nav>
                    <div class="flex space-x-6">
                        <RouterLink to="/" class="py-2 px-3 text-gray-700 hover:text-blue-600 font-medium hover:bg-blue-50 rounded-md transition-colors">数据看板</RouterLink>
                        <RouterLink to="/proxy" class="py-2 px-3 text-gray-700 hover:text-blue-600 font-medium hover:bg-blue-50 rounded-md transition-colors">客户端管理</RouterLink>
                        <RouterLink to="/proxy" class="py-2 px-3 text-gray-700 hover:text-blue-600 font-medium hover:bg-blue-50 rounded-md transition-colors">账号管理</RouterLink>
                    </div>
                </nav>
            </div>
        </header>

        <!-- 主体内容区 -->
        <div class="flex-1 mt-6 px-6">
            <!-- 内容区域 -->
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
</template>

<style scoped>
/* 可以在这里添加任何不能通过Tailwind实现的自定义样式 */
</style>
