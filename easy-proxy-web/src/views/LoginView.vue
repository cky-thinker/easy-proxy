<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div>
        <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
          登录到 Easy Proxy
        </h2>
        <p class="mt-2 text-center text-sm text-gray-600">
          请输入您的账户信息
        </p>
      </div>
      
      <form class="mt-8 space-y-6" @submit.prevent="handleLogin">
        <div class="rounded-md shadow-sm -space-y-px">
          <div>
            <label for="username" class="sr-only">用户名</label>
            <input
              id="username"
              v-model="loginForm.username"
              name="username"
              type="text"
              required
              class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
              placeholder="用户名"
            />
          </div>
          
          <div>
            <label for="password" class="sr-only">密码</label>
            <input
              id="password"
              v-model="loginForm.password"
              name="password"
              type="password"
              required
              class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
              :class="{ 'rounded-b-md': !captchaEnabled }"
              placeholder="密码"
            />
          </div>
          
          <div v-if="captchaEnabled" class="flex">
            <input
              id="captchaCode"
              v-model="loginForm.captchaCode"
              name="captchaCode"
              type="text"
              :required="captchaEnabled"
              class="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-bl-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 focus:z-10 sm:text-sm"
              placeholder="验证码"
            />
            <div class="relative">
              <img
                v-if="captchaImage"
                :src="captchaImage.img"
                alt="验证码"
                class="h-10 w-24 border border-l-0 border-gray-300 rounded-br-md cursor-pointer"
                @click="refreshCaptcha"
                title="点击刷新验证码"
              />
              <div
                v-else
                class="h-10 w-24 border border-l-0 border-gray-300 rounded-br-md bg-gray-100 flex items-center justify-center cursor-pointer"
                @click="refreshCaptcha"
              >
                <span class="text-xs text-gray-500">点击获取</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="errorMessage" class="text-red-600 text-sm text-center">
          {{ errorMessage }}
        </div>

        <div>
          <button
            type="submit"
            :disabled="isLoading"
            class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="isLoading" class="absolute left-0 inset-y-0 flex items-center pl-3">
              <svg class="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            </span>
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useAuthStore } from '../stores/auth';
import type { LoginRequest, CaptchaImage } from '../api/types';

const authStore = useAuthStore();

// 响应式数据
const loginForm = ref<LoginRequest>({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: '',
});

const captchaImage = ref<CaptchaImage | null>(null);
const errorMessage = ref('');
const isLoading = ref(false);
const captchaEnabled = computed(() => authStore.serverConfig?.captchaImageEnable === true);

// 获取验证码
const refreshCaptcha = async () => {
  try {
    captchaImage.value = await authStore.fetchCaptcha();
    loginForm.value.captchaId = captchaImage.value.captchaId;
    loginForm.value.captchaCode = ''; // 清空验证码输入
  } catch (error) {
    console.error('获取验证码失败:', error);
    errorMessage.value = '获取验证码失败，请重试';
  }
};

// 处理登录
const handleLogin = async () => {
  if (captchaEnabled.value) {
    if (!captchaImage.value) {
      errorMessage.value = '请先获取验证码';
      return;
    }
  }

  errorMessage.value = '';
  isLoading.value = true;

  try {
    await authStore.login(loginForm.value);
    
    // 登录成功，跳转到首页
    window.location.href = '/';
  } catch (error: any) {
    console.error('登录失败:', error);
    
    // 显示错误信息
    if (error.response?.data?.msg) {
      errorMessage.value = error.response.data.msg;
    } else if (error.response?.data?.error) {
      errorMessage.value = error.response.data.error;
    } else {
      errorMessage.value = '登录失败，请检查用户名和密码';
    }
    
    // 刷新验证码（仅当启用时）
    if (captchaEnabled.value) {
      await refreshCaptcha();
    }
  } finally {
    isLoading.value = false;
  }
};

// 组件挂载时获取验证码
onMounted(async () => {
  // 先获取服务端配置
  await authStore.fetchServerConfig();
  // 若开启验证码，则加载一次验证码
  if (captchaEnabled.value) {
    await refreshCaptcha();
  }
});
</script>