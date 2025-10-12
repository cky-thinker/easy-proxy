import apiClient from '../util/client'
import type { 
  ProxyClientConfig, 
  ProxyRule,
  ApiResponse,
  PageResult,
  ProxyRuleQuery
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
}

// 获取客户端列表（支持服务端分页与筛选）
export const getClients = async (
  page: number = 1,
  pageSize: number = 10,
  q?: string,
  status?: 'online' | 'offline',
  enableFlag?: boolean
): Promise<PageResult<ExtendedProxyClientConfig>> => {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })
  if (q) params.append('q', q)
  if (status) params.append('status', status)
  if (enableFlag !== undefined) params.append('enableFlag', String(enableFlag))
  const response = await apiClient.get<ApiResponse<PageResult<ExtendedProxyClientConfig>>>(`/api/proxyClient?${params.toString()}`)
  return response.data.data
}

// 获取所有客户端（不分页，用于下拉列表）
export const getAllClients = async (): Promise<ExtendedProxyClientConfig[]> => {
  const response = await apiClient.get<ApiResponse<ExtendedProxyClientConfig[]>>('/api/proxyClient/all')
  return response.data.data
}

// 获取单个客户端详情
export const getClient = async (id: number): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.get<ApiResponse<ExtendedProxyClientConfig>>(`/api/proxyClient/${id}`)
  return response.data.data
}

// 创建客户端
export const createClient = async (clientData: Pick<ExtendedProxyClientConfig, 'name' | 'token' | 'enableFlag'>): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.post<ApiResponse<ExtendedProxyClientConfig>>('/api/proxyClient', clientData)
  return response.data.data
}

// 更新客户端
export const updateClient = async (id: number, clientData: Partial<ExtendedProxyClientConfig>): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.put<ApiResponse<ExtendedProxyClientConfig>>(`/api/proxyClient/${id}`, { id, ...clientData })
  return response.data.data
}

// 删除客户端
export const deleteClient = async (id: number): Promise<void> => {
  await apiClient.delete(`/api/proxyClient/${id}`)
}

// 批量删除客户端
// 后端暂未提供批量删除端点，保留占位以便未来扩展
export const deleteClients = async (_ids: number[]): Promise<void> => {
  console.warn('Batch delete not implemented on server')
}

// 切换客户端状态
// 后端未提供单独切换状态端点，使用更新接口
export const toggleClientStatus = async (id: number, enableFlag: boolean): Promise<ExtendedProxyClientConfig> => {
  return updateClient(id, { enableFlag })
}

// 获取客户端代理规则（支持按名称、端口、客户端ID过滤）
export const getClientRules = async (
  params: ProxyRuleQuery
): Promise<ProxyRule[]> => {
  const response = await apiClient.get<ApiResponse<ProxyRule[]>>(`/api/proxyClientRule/all`, { params })
  return response.data.data
}

// 获取代理规则分页
export const getClientRulesPage = async (
  params: ProxyRuleQuery
): Promise<PageResult<ProxyRule>> => {
  const response = await apiClient.get<ApiResponse<PageResult<ProxyRule>>>(`/api/proxyClientRule`, { params })
  return response.data.data
}

// 添加代理规则
export const addClientRule = async (clientId: number, rule: Omit<ProxyRule, 'id'>): Promise<ProxyRule> => {
  const payload = { ...rule, proxyClientId: clientId }
  const response = await apiClient.post<ApiResponse<ProxyRule>>('/api/proxyClientRule', payload)
  return response.data.data
}

// 更新代理规则
export const updateClientRule = async (_clientId: number, ruleId: number, rule: Partial<ProxyRule>): Promise<ProxyRule> => {
  const payload = { id: ruleId, ...rule }
  const response = await apiClient.put<ApiResponse<ProxyRule>>('/api/proxyClientRule', payload)
  return response.data.data
}

// 删除代理规则
export const deleteClientRule = async (_clientId: number, ruleId: number): Promise<void> => {
  await apiClient.delete(`/api/proxyClientRule/${ruleId}`)
}

// 搜索客户端
export const searchClients = async (name: string, page: number = 1, pageSize: number = 10): Promise<PageResult<ExtendedProxyClientConfig>> => {
  return getClients(page, pageSize, name)
}

// 获取客户端统计信息
// 统计端点存在于 /api/traffic/...，此处保留占位以供仪表盘使用具体统计API
export const getClientStats = async (_id: number, _period: 'day' | 'week' | 'month' = 'day'): Promise<{ traffic: any[], connections: any[] }> => {
  console.warn('Use TrafficStatisticController endpoints instead: /api/traffic/*')
  return { traffic: [], connections: [] }
}