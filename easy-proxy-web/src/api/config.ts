import apiClient from '../util/client'
import type { ApiResponse, ServerConfig } from './types'

// 获取服务端配置
export const getServerConfig = async (): Promise<ServerConfig> => {
  const response = await apiClient.get<ApiResponse<ServerConfig>>('/api/sys/config')
  return response.data.data
}