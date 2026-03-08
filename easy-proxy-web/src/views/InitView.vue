<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full space-y-8">
      <div>
        <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
          系统初始化
        </h2>
        <p class="mt-2 text-center text-sm text-gray-600">
          请设置系统管理员账号
        </p>
      </div>

      <el-form class="mt-8 space-y-6" :model="form" :rules="rules" ref="formRef" @submit.prevent="handleSubmit" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>

        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="isLoading" native-type="submit" class="w-full">初始化系统</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { initUser } from '../api/auth';

const router = useRouter();
const formRef = ref<FormInstance>();
const isLoading = ref(false);

const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  mobile: '',
  email: ''
});

const validatePass2 = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== form.value.password) {
    callback(new Error('两次输入密码不一致!'));
  } else {
    callback();
  }
};

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      isLoading.value = true;
      try {
        await initUser({
            username: form.value.username,
            password: form.value.password,
            mobile: form.value.mobile,
            email: form.value.email
        });
        ElMessage.success('系统初始化成功，请登录');
        router.push('/login');
      } catch (error: any) {
        ElMessage.error(error.response?.data?.msg || '初始化失败');
      } finally {
        isLoading.value = false;
      }
    }
  });
};
</script>
