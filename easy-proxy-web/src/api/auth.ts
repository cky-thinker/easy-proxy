import apiClient from '../util/client';
import type { LoginRequest, CaptchaImage, UserInfo, ApiResponse } from './types';

/**
 * 获取验证码图片
 */
export const getCaptchaImage = async (): Promise<CaptchaImage> => {
  const response = await apiClient.get<ApiResponse<CaptchaImage>>('/api/open/captchaImage');
  return response.data.data;
};

/**
 * 用户登录
 */
export const loginUser = async (loginData: LoginRequest): Promise<UserInfo> => {
  const response = await apiClient.post<ApiResponse<UserInfo>>('/api/open/loginUser', loginData);
  return response.data.data;
};

/**
 * 检查系统是否需要初始化
 */
export const checkInit = async (): Promise<boolean> => {
  const response = await apiClient.get<ApiResponse<boolean>>('/api/open/checkInit');
  return response.data.data;
};

/**
 * 初始化系统管理员
 */
export const initUser = async (user: any): Promise<UserInfo> => {
  const response = await apiClient.post<ApiResponse<UserInfo>>('/api/open/initUser', user);
  return response.data.data;
};

/**
 * 用户登出
 */
export const logoutUser = async (): Promise<void> => {
  // 清除本地存储的token
  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');
};