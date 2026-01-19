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
      
      <el-form class="mt-8 space-y-6" :model="loginForm" :rules="rules" ref="loginFormRef" @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" autocomplete="current-password" show-password />
        </el-form-item>

        <el-form-item v-if="captchaEnabled" label="验证码" prop="captchaCode">
          <div class="flex">
            <el-input v-model="loginForm.captchaCode" placeholder="请输入验证码" />
            <img
              v-if="captchaImage"
              :src="captchaImage.img"
              alt="验证码"
              class="h-10 w-24 ml-2 border rounded-md cursor-pointer"
              @click="refreshCaptcha"
            />
            <div
              v-else
              class="h-10 w-24 ml-2 border rounded-md bg-gray-100 flex items-center justify-center cursor-pointer"
              @click="refreshCaptcha"
            >
              <span class="text-xs text-gray-500">点击获取</span>
            </div>
          </div>
        </el-form-item>

        <div v-if="errorMessage" class="text-red-600 text-sm text-center">
          {{ errorMessage }}
        </div>

        <el-form-item>
          <el-button type="primary" :loading="isLoading" native-type="submit" class="w-full">登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
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