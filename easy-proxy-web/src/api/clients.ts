import apiClient from './client'
import type { 
  ProxyClientConfig, 
  ProxyRule,
  ApiResponse 
} from './types'

// 扩展客户端配置接口
export interface ExtendedProxyClientConfig extends ProxyClientConfig {
  id: number
  status: 'online' | 'offline'
  lastSeen?: string
  traffic: {
    upload: number
    download: number
  }
  connections: number
  enableFlag: boolean
}

// 获取客户端列表
export const getClients = async (page: number = 1, limit: number = 10): Promise<{ clients: ExtendedProxyClientConfig[], total: number }> => {
  const response = await apiClient.get<ApiResponse<{ clients: ExtendedProxyClientConfig[], total: number }>>(`/api/clients?page=${page}&limit=${limit}`)
  return response.data.data
}

// 获取单个客户端详情
export const getClient = async (id: number): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.get<ApiResponse<ExtendedProxyClientConfig>>(`/api/clients/${id}`)
  return response.data.data
}

// 创建客户端
export const createClient = async (clientData: Omit<ExtendedProxyClientConfig, 'id' | 'status' | 'lastSeen' | 'traffic' | 'connections'>): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.post<ApiResponse<ExtendedProxyClientConfig>>('/api/clients', clientData)
  return response.data.data
}

// 更新客户端
export const updateClient = async (id: number, clientData: Partial<ExtendedProxyClientConfig>): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.put<ApiResponse<ExtendedProxyClientConfig>>(`/api/clients/${id}`, clientData)
  return response.data.data
}

// 删除客户端
export const deleteClient = async (id: number): Promise<void> => {
  await apiClient.delete(`/api/clients/${id}`)
}

// 批量删除客户端
export const deleteClients = async (ids: number[]): Promise<void> => {
  await apiClient.post('/api/clients/batch-delete', { ids })
}

// 切换客户端状态
export const toggleClientStatus = async (id: number, enableFlag: boolean): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.patch<ApiResponse<ExtendedProxyClientConfig>>(`/api/clients/${id}/status`, { enableFlag })
  return response.data.data
}

// 获取客户端代理规则
export const getClientRules = async (clientId: number): Promise<ProxyRule[]> => {
  const response = await apiClient.get<ApiResponse<ProxyRule[]>>(`/api/clients/${clientId}/rules`)
  return response.data.data
}

// 添加代理规则
export const addClientRule = async (clientId: number, rule: Omit<ProxyRule, 'id'>): Promise<ProxyRule> => {
  const response = await apiClient.post<ApiResponse<ProxyRule>>(`/api/clients/${clientId}/rules`, rule)
  return response.data.data
}

// 更新代理规则
export const updateClientRule = async (clientId: number, ruleId: number, rule: Partial<ProxyRule>): Promise<ProxyRule> => {
  const response = await apiClient.put<ApiResponse<ProxyRule>>(`/api/clients/${clientId}/rules/${ruleId}`, rule)
  return response.data.data
}

// 删除代理规则
export const deleteClientRule = async (clientId: number, ruleId: number): Promise<void> => {
  await apiClient.delete(`/api/clients/${clientId}/rules/${ruleId}`)
}

// 搜索客户端
export const searchClients = async (query: string, filters?: { status?: string, type?: string }): Promise<ExtendedProxyClientConfig[]> => {
  const params = new URLSearchParams({ q: query })
  if (filters?.status) params.append('status', filters.status)
  if (filters?.type) params.append('type', filters.type)
  
  const response = await apiClient.get<ApiResponse<ExtendedProxyClientConfig[]>>(`/api/clients/search?${params.toString()}`)
  return response.data.data
}

// 获取客户端统计信息
export const getClientStats = async (id: number, period: 'day' | 'week' | 'month' = 'day'): Promise<{ traffic: any[], connections: any[] }> => {
  const response = await apiClient.get<ApiResponse<{ traffic: any[], connections: any[] }>>(`/api/clients/${id}/stats?period=${period}`)
  return response.data.data
}