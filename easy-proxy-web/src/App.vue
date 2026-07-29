<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

// 初始化认证状态
authStore.initAuth()
</script>

<template><!-- 如果不在登录/初始化页面 -->
<template v-if="!['/login', '/init'].includes(route.path)">
    <!-- 已登录：显示正常骨架 -->
    <div v-if="authStore.isLoggedIn" class="flex flex-col min-h-screen bg-gray-50">
        <NavBar />

        <div class="flex-1 relative z-10 flex flex-col">
            <main class="flex-1 flex flex-col">
                <RouterView />
            </main>
        </div>
    </div>
    <!-- 未登录：显示临时提示页 -->
    <div v-else class="flex items-center justify-center min-h-screen bg-gray-50 p-4">
        <div class="max-w-md w-full bg-white rounded-xl shadow-lg p-8 text-center border border-gray-100">
            <h2 class="text-2xl font-bold text-gray-900 mb-2">需要登录</h2>
            <p class="text-gray-600 mb-8">
                该页面需要登录后才能访问。请先登录您的账号。
            </p>
            <div class="space-y-3">
                <el-button type="primary" class="w-full h-11! text-base font-medium" @click="router.push('/login')">
                    立即登录
                </el-button>
            </div>
        </div>
    </div>
</template>

<!-- 在登录/初始化页面：不显示骨架，直接渲染 RouterView -->
<div v-else class="min-h-screen bg-gray-50">
    <RouterView />
</div>
</template>

<style scoped>
/* 可以在这里添加任何不能通过Tailwind实现的自定义样式 */
</style>
