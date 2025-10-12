import apiClient from '../util/client'
import type { 
  DashboardStats, 
  TrafficRanking, 
  TrafficTrend, 
  RecentActivity,
  ApiResponse 
} from './types'

// 获取仪表板统计数据
export const getDashboardStats = async (): Promise<DashboardStats> => {
  const response = await apiClient.get<ApiResponse<DashboardStats>>('/api/dashboard/stats')
  return response.data.data
}

// 获取流量排行
export const getTrafficRanking = async (period: 'day' | 'week' | 'month' = 'day'): Promise<TrafficRanking[]> => {
  const response = await apiClient.get<ApiResponse<TrafficRanking[]>>(`/api/dashboard/traffic-ranking?period=${period}`)
  return response.data.data
}

// 获取流量趋势数据
export const getTrafficTrend = async (period: 'day' | 'week' | 'month' = 'day'): Promise<TrafficTrend[]> => {
  const response = await apiClient.get<ApiResponse<TrafficTrend[]>>(`/api/dashboard/traffic-trend?period=${period}`)
  return response.data.data
}

// 获取最近活动
export const getRecentActivities = async (limit: number = 10): Promise<RecentActivity[]> => {
  const response = await apiClient.get<ApiResponse<RecentActivity[]>>(`/api/dashboard/activities?limit=${limit}`)
  return response.data.data
}

// 导出实时数据
export const exportDashboardData = async (type: 'stats' | 'traffic' | 'activities', format: 'csv' | 'excel' = 'csv'): Promise<Blob> => {
  const response = await apiClient.get(`/api/dashboard/export?type=${type}&format=${format}`, {
    responseType: 'blob'
  })
  return response.data
}