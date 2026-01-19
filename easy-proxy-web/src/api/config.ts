import apiClient from '../util/client'
import type { ApiResponse, ServerConfig } from './types'

// 获取登录配置
export const getLoginConfig = async (): Promise<ServerConfig> => {
  const response = await apiClient.get<ApiResponse<ServerConfig>>('/api/open/loginConfig')
  return response.data.data
}