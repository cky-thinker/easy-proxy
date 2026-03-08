<template>
  <div class="min-h-screen flex bg-gray-50 lg:bg-white">
    <!-- Left Side - Architecture Diagram (Only on Large Screens) -->
    <div class="hidden lg:flex lg:w-7/12 bg-gradient-to-br from-blue-600 to-indigo-900 flex-col items-center justify-center p-12 relative overflow-hidden">
      <div class="relative z-10 w-full max-w-4xl flex flex-col items-center">
        <h1 class="text-4xl font-bold text-white mb-2 tracking-wide">Easy Proxy</h1>
        <p class="text-blue-100 text-lg mb-12 font-light">安全 · 高效 · 稳定 的内网穿透服务</p>
        <div class="w-full aspect-[4/3] bg-white/5 backdrop-blur-sm rounded-3xl p-4 shadow-2xl border border-white/10">
          <LoginArchitecture />
        </div>
      </div>
      
      <!-- Decorative Background Elements -->
      <div class="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div class="absolute -top-[20%] -left-[10%] w-[50%] h-[50%] bg-blue-500 rounded-full mix-blend-overlay filter blur-[100px] opacity-30"></div>
        <div class="absolute -bottom-[20%] -right-[10%] w-[50%] h-[50%] bg-indigo-500 rounded-full mix-blend-overlay filter blur-[100px] opacity-30"></div>
      </div>
    </div>

    <!-- Right Side - Login Form -->
    <div class="w-full lg:w-5/12 flex items-center justify-center p-4 sm:p-6 lg:p-12">
      <div class="max-w-md w-full space-y-8 bg-white p-8 rounded-2xl shadow-xl lg:shadow-none border border-gray-100 lg:border-none">
        <div>
          <h2 class="mt-2 text-center text-3xl font-extrabold text-gray-900 lg:text-left">
            欢迎回来
          </h2>
          <p class="mt-2 text-center text-sm text-gray-600 lg:text-left">
            登录到您的 Easy Proxy 控制台
          </p>
        </div>
        
        <el-form class="mt-8 space-y-6" :model="loginForm" :rules="rules" ref="loginFormRef" @submit.prevent="handleLogin" label-position="top" size="large">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" autocomplete="username">
              <template #prefix>
                <el-icon class="text-gray-400"><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" autocomplete="current-password" show-password>
              <template #prefix>
                <el-icon class="text-gray-400"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item v-if="captchaEnabled" label="验证码" prop="captchaCode">
            <div class="flex w-full gap-3">
              <el-input v-model="loginForm.captchaCode" placeholder="验证码" class="flex-1">
                <template #prefix>
                  <el-icon class="text-gray-400"><Key /></el-icon>
                </template>
              </el-input>
              <div class="h-10 w-32 flex-shrink-0">
                <img
                  v-if="captchaImage"
                  :src="captchaImage.img"
                  alt="验证码"
                  class="h-full w-full object-cover border border-gray-200 rounded-md cursor-pointer hover:opacity-80 transition-opacity"
                  @click="refreshCaptcha"
                  title="点击刷新"
                />
                <div
                  v-else
                  class="h-full w-full border border-gray-200 rounded-md bg-gray-50 flex items-center justify-center cursor-pointer text-gray-400 hover:bg-gray-100 transition-colors"
                  @click="refreshCaptcha"
                >
                  <span class="text-xs">点击获取</span>
                </div>
              </div>
            </div>
          </el-form-item>

          <div v-if="errorMessage" class="p-3 rounded-md bg-red-50 text-red-600 text-sm text-center border border-red-100 flex items-center justify-center gap-2">
            <el-icon><Warning /></el-icon>
            {{ errorMessage }}
          </div>

          <el-form-item class="pt-2">
            <el-button type="primary" :loading="isLoading" native-type="submit" class="w-full !h-12 !text-lg !rounded-lg shadow-md hover:shadow-lg transition-all duration-300">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { User, Lock, Key, Warning } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';
import type { LoginRequest, CaptchaImage } from '../api/types';
import LoginArchitecture from '../components/LoginArchitecture.vue';

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

// Element Plus 表单引用与校验规则
const loginFormRef = ref<FormInstance>()
const rules: FormRules<LoginRequest> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{
    validator: (_rule, value, callback) => {
      if (captchaEnabled.value && !value) {
        callback(new Error('请输入验证码'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
}

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
  if (captchaEnabled.value && !captchaImage.value) {
    errorMessage.value = '请先获取验证码'
    ElMessage.warning('请先获取验证码')
    return
  }

  errorMessage.value = ''
  isLoading.value = true

  try {
    await loginFormRef.value?.validate?.()
    await authStore.login(loginForm.value)
    ElMessage.success('登录成功')
    // 登录成功，跳转到首页
    window.location.href = '/'
  } catch (error: any) {
    console.error('登录失败:', error)

    // 显示错误信息
    if (error?.response?.data?.msg) {
      errorMessage.value = error.response.data.msg
    } else if (error?.response?.data?.error) {
      errorMessage.value = error.response.data.error
    } else {
      errorMessage.value = '登录失败，请检查用户名和密码'
    }
    ElMessage.error(errorMessage.value)

    // 刷新验证码（仅当启用时）
    if (captchaEnabled.value) {
      await refreshCaptcha()
    }
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时获取验证码
onMounted(async () => {
  // 先获取服务端配置
  await authStore.fetchLoginConfig();
  // 若开启验证码，则加载一次验证码
  if (captchaEnabled.value) {
    await refreshCaptcha();
  }
});
</script>