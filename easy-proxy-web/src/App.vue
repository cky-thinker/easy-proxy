<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()
const route = useRoute()

// 初始化认证状态
onMounted(() => {
  authStore.initAuth()
})
</script>

<template>
    <div v-if="!['/login', '/init'].includes(route.path)" class="flex flex-col min-h-screen bg-gray-50">
        <NavBar />

        <div class="flex-1 relative z-10 flex flex-col">
            <main class="flex-1 flex flex-col">
                <RouterView />
            </main>
        </div>
    </div>
    <div v-else class="min-h-screen bg-gray-50">
        <RouterView />
    </div>
</template>

<style scoped>
/* 可以在这里添加任何不能通过Tailwind实现的自定义样式 */
</style>
