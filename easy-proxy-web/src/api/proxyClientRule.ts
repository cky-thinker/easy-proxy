import apiClient from '../util/client'
import type { ProxyRule, ApiResponse, PageResult, ProxyRuleQuery } from './types'

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