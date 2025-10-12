<template>
  <nav class="bg-white shadow-lg relative z-20">
    <div class="px-4">
      <div class="flex justify-between h-16">
        <div class="flex items-center">
          <!-- Logo + Title -->
          <router-link to="/" class="flex-shrink-0 flex items-center space-x-2">
            <img src="/favicon.ico" alt="logo" class="w-6 h-6" />
            <h1 class="text-xl font-bold text-gray-800">Easy Proxy</h1>
          </router-link>

          <!-- Navigation Links -->
          <div class="hidden md:ml-6 md:flex md:space-x-0 h-16">
            <router-link v-if="authStore.isLoggedIn" to="/"
              class="text-black hover:bg-[var(--color-blue-500)] hover:text-white min-w-[100px] px-3 py-2 text-base font-medium transition-colors h-full flex items-center justify-center"
              active-class="bg-[var(--color-blue-600)] text-white">
              总览
            </router-link>
            <router-link v-if="authStore.isLoggedIn" to="/clients"
              class="text-black hover:bg-[var(--color-blue-500)] hover:text-white min-w-[100px] px-3 py-2 text-base font-medium transition-colors h-full flex items-center justify-center"
              active-class="bg-[var(--color-blue-600)] text-white">
              客户端管理
            </router-link>
            <router-link v-if="authStore.isLoggedIn" to="/accounts"
              class="text-black hover:bg-[var(--color-blue-500)] hover:text-white min-w-[100px] px-3 py-2 text-base font-medium transition-colors h-full flex items-center justify-center"
              active-class="bg-[var(--color-blue-600)] text-white">
              账号管理
            </router-link>
          </div>
        </div>

        <!-- User Menu -->
        <div class="flex items-center space-x-4">
          <div v-if="authStore.isLoggedIn" class="flex items-center space-x-4">
            <!-- User Info + Dropdown -->
            <div class="relative">
              <div class="flex items-center space-x-2">
                <div class="w-8 h-8 bg-indigo-500 rounded-full flex items-center justify-center">
                  <span class="text-white text-sm font-medium">
                    {{ userInitial }}
                  </span>
                </div>
                <span class="text-gray-700 text-sm font-medium">
                  {{ authStore.userInfo?.username }}
                </span>
                <button @click="toggleUserMenu" class="p-1 text-gray-600 hover:text-gray-800 cursor-pointer" aria-label="用户菜单" :aria-expanded="showUserMenu">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
              </div>
              <div v-show="showUserMenu"
                class="absolute right-0 top-full mt-2 w-36 bg-white border border-gray-200 shadow-lg">
                <button @click="goToManage"
                  class="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-[var(--color-blue-500)]/20 cursor-pointer">管理</button>
                <button @click="handleLogout"
                  class="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-100 cursor-pointer">登出</button>
              </div>
            </div>
          </div>

          <!-- Login Button -->
          <router-link v-else to="/login"
            class="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium transition-colors">
            登录
          </router-link>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const router = useRouter();
const showUserMenu = ref(false);

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

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value;
};

const goToManage = () => {
  router.push('/accounts');
  showUserMenu.value = false;
};
</script>
