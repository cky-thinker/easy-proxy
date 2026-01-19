import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import type { UserInfo, LoginRequest } from '@/api/types';
import { loginUser, logoutUser, getCaptchaImage } from '@/api/auth';
import { getLoginConfig } from '@/api/config';
import type { ServerConfig } from '@/api/types';

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const userInfo = ref<UserInfo | null>(null);
  const token = ref<string>('');
  const isLoading = ref(false);
  const serverConfig = ref<ServerConfig | null>(null);

  // 计算属性
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value);

  // 初始化：从localStorage恢复状态
  const initAuth = () => {
    const savedToken = localStorage.getItem('token');
    const savedUserInfo = localStorage.getItem('userInfo');
    
    if (savedToken && savedUserInfo) {
      token.value = savedToken;
      userInfo.value = JSON.parse(savedUserInfo);
    }
  };

  // 获取服务端配置
  const fetchLoginConfig = async (): Promise<ServerConfig> => {
    const cfg = await getLoginConfig();
    serverConfig.value = cfg;
    return cfg;
  };

  // 登录
  const login = async (loginData: LoginRequest): Promise<void> => {
    isLoading.value = true;
    try {
      const userData = await loginUser(loginData);
      
      // 保存用户信息和token
      userInfo.value = userData;
      token.value = userData.token;
      
      // 持久化到localStorage
      localStorage.setItem('token', userData.token);
      localStorage.setItem('userInfo', JSON.stringify(userData));
      
    } catch (error) {
      throw error;
    } finally {
      isLoading.value = false;
    }
  };

  // 登出
  const logout = async (): Promise<void> => {
    await logoutUser();
    
    // 清除状态
    userInfo.value = null;
    token.value = '';
  };

  // 获取验证码
  const fetchCaptcha = async () => {
    return await getCaptchaImage();
  };

  return {
    // 状态
    userInfo,
    token,
    isLoading,
    serverConfig,
    
    // 计算属性
    isLoggedIn,
    
    // 方法
    initAuth,
    fetchLoginConfig: fetchLoginConfig,
    login,
    logout,
    fetchCaptcha,
  };
});