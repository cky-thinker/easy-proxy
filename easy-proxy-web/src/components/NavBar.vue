<template>
  <nav class="bg-white shadow-lg">
    <div class="max-w-7xl mx-auto px-4">
      <div class="flex justify-between h-16">
        <div class="flex items-center">
          <!-- Logo -->
          <router-link to="/" class="flex-shrink-0 flex items-center">
            <h1 class="text-xl font-bold text-gray-800">Easy Proxy</h1>
          </router-link>
          
          <!-- Navigation Links -->
          <div class="hidden md:ml-6 md:flex md:space-x-8">
            <router-link
              to="/"
              class="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium"
              active-class="text-indigo-600 border-b-2 border-indigo-600"
            >
              首页
            </router-link>
            <router-link
              v-if="authStore.isLoggedIn"
              to="/proxy"
              class="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium"
              active-class="text-indigo-600 border-b-2 border-indigo-600"
            >
              代理管理
            </router-link>
          </div>
        </div>
        
        <!-- User Menu -->
        <div class="flex items-center space-x-4">
          <div v-if="authStore.isLoggedIn" class="flex items-center space-x-4">
            <!-- User Info -->
            <div class="flex items-center space-x-2">
              <div class="w-8 h-8 bg-indigo-500 rounded-full flex items-center justify-center">
                <span class="text-white text-sm font-medium">
                  {{ userInitial }}
                </span>
              </div>
              <span class="text-gray-700 text-sm font-medium">
                {{ authStore.userInfo?.username }}
              </span>
            </div>
            
            <!-- Logout Button -->
            <button
              @click="handleLogout"
              class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
            >
              登出
            </button>
          </div>
          
          <!-- Login Button -->
          <router-link
            v-else
            to="/login"
            class="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors"
          >
            登录
          </router-link>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();

// 计算用户名首字母
const userInitial = computed(() => {
  return authStore.userInfo?.username?.charAt(0).toUpperCase() || 'U';
});

// 处理登出
const handleLogout = async () => {
  try {
    await authStore.logout();
    // 登出后刷新页面到登录页
    window.location.href = '/login';
  } catch (error) {
    console.error('登出失败:', error);
  }
};
</script>