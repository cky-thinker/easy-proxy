import apiClient from '@/util/client'
import type { DashboardStats, TrafficRanking, TrafficTrend, RecentActivity } from './types'

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const res = await apiClient.get('/api/dashboard/stats')
  return res.data.data as DashboardStats
}

export const getTrafficRanking = async (period: 'day'|'week'|'month'): Promise<TrafficRanking[]> => {
  const res = await apiClient.get(`/api/dashboard/trafficRanking`, { params: { period } })
  return res.data.data as TrafficRanking[]
}

export const getTrafficTrend = async (period: 'day'|'week'|'month'): Promise<TrafficTrend[]> => {
  const res = await apiClient.get(`/api/dashboard/trafficTrend`, { params: { period } })
  return res.data.data as TrafficTrend[]
}

export const getRecentActivities = async (): Promise<RecentActivity[]> => {
  const res = await apiClient.get(`/api/dashboard/recentActivities`)
  return res.data.data as RecentActivity[]
}