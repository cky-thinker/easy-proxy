import apiClient from '../util/client'
import type { ProxyRule, ApiResponse, PageResult, ProxyRuleQuery, RuleRealtimeTraffic, TrafficTrend } from './types'

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
  await apiClient.delete(`/api/proxyClientRule`, { params: { id: ruleId } })
}

// 获取规则实时流量
export const getRuleRealtimeTraffic = async (ruleId: number): Promise<RuleRealtimeTraffic> => {
  const response = await apiClient.get<ApiResponse<RuleRealtimeTraffic>>('/api/traffic/realtime', {
    params: { proxyClientRuleId: ruleId }
  })
  return response.data.data
}

// 获取规则流量趋势
export const getRuleTrafficTrend = async (
  ruleId: number, 
  period: 'day' | 'week' | 'month'
): Promise<TrafficTrend[]> => {
  let url = ''
  const params: any = { 
    proxyClientRuleId: ruleId,
    pageSize: 30,
    page: 1 // Assuming default page is 1
  }
  
  const now = new Date()
  const startDate = new Date(now)
  
  if (period === 'day') {
    url = '/api/traffic/hourReport'
    startDate.setDate(now.getDate() - 1)
  } else {
    // For week and month, use day report
    url = '/api/traffic/dayReport'
    if (period === 'week') {
      startDate.setDate(now.getDate() - 7)
    } else {
      startDate.setDate(now.getDate() - 30)
    }
  }
  
  const formatDate = (d: Date) => {
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  
  params.startDate = formatDate(startDate)
  params.endDate = formatDate(now)
  
  const response = await apiClient.get<ApiResponse<PageResult<any>>>(url, { params })
  const list = response.data.data.list || []
  
  // Transform to TrafficTrend
  // Backend returns TsHourReport or TsDayReport which has date (Date/number), uploadBytes, downloadBytes
  return list.map((item: any) => ({
    time: item.date,
    upload: item.uploadBytes || 0,
    download: item.downloadBytes || 0
  })).sort((a: any, b: any) => new Date(a.time).getTime() - new Date(b.time).getTime())
}
