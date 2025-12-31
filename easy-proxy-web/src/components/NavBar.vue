<template>
  <nav class="bg-white shadow-lg relative z-20">
    <div class="px-4">
      <div class="flex justify-between items-center">
        <div class="flex items-center space-x-4">
          <!-- Logo + Title -->
          <router-link to="/" class="flex-shrink-0 flex items-center space-x-2">
            <img src="/favicon.ico" alt="logo" class="w-6 h-6" />
            <h1 class="text-xl font-bold text-gray-800">Easy Proxy</h1>
          </router-link>

          <!-- Navigation Menu -->
          <el-menu v-if="authStore.isLoggedIn" mode="horizontal" :default-active="route.path" :ellipsis="false" class="border-0 bg-transparent">
            <el-menu-item index="/" @click="router.push('/')">系统总览</el-menu-item>
            <el-menu-item index="/clients" @click="router.push('/clients')">客户端管理</el-menu-item>
            <el-menu-item index="/clientRules" @click="router.push('/clientRules')">规则管理</el-menu-item>
            <el-menu-item index="/accounts" @click="router.push('/accounts')">账号管理</el-menu-item>
          </el-menu>
        </div>

        <!-- User Menu / Login -->
        <div class="flex items-center">
          <template v-if="authStore.isLoggedIn">
            <el-dropdown @command="onUserCommand">
              <span class="el-dropdown-link flex items-center space-x-2 cursor-pointer">
                <div class="w-8 h-8 bg-indigo-500 rounded-full flex items-center justify-center">
                  <span class="text-white text-sm font-medium">{{ userInitial }}</span>
                </div>
                <span class="text-gray-700 text-sm font-medium">{{ authStore.userInfo?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">登出</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

// 计算用户名首字母
const userInitial = computed(() => {
  return authStore.userInfo?.username?.charAt(0).toUpperCase() || 'U';
});

const onUserCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await authStore.logout();
      window.location.href = '/login';
    } catch (error) {
      console.error('登出失败:', error);
    }
  }
};
</script>
