<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted } from 'vue'
import NavBar from './components/NavBar.vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()

// 初始化认证状态
onMounted(() => {
  authStore.initAuth()
})
</script>

<template>
    <div class="flex flex-col min-h-screen bg-gray-50">
        <!-- 导航栏 -->
        <NavBar />

        <!-- 主体内容区 -->
        <div class="flex-1 relative z-10">
            <!-- 内容区域 -->
            <main class="min-h-[calc(100vh-64px)]">
                <RouterView />
            </main>
        </div>
    </div>
</template>

<style scoped>
/* 可以在这里添加任何不能通过Tailwind实现的自定义样式 */
</style>
