import apiClient from '../util/client'
import type { 
  User, 
  CreateUserRequest, 
  UpdateUserRequest,
  Permission,
  ApiResponse,
  PageResult 
} from './types'

// 获取账号列表（分页）
export const getUsers = async (page: number = 1, pageSize: number = 10): Promise<PageResult<User>> => {
  const response = await apiClient.get<ApiResponse<PageResult<User>>>(`/api/users?page=${page}&pageSize=${pageSize}`)
  return response.data.data
}

// 获取单个账号详情
export const getUser = async (id: number): Promise<User> => {
  const response = await apiClient.get<ApiResponse<User>>(`/api/users/${id}`)
  return response.data.data
}

// 创建账号
export const createUser = async (accountData: CreateUserRequest): Promise<User> => {
  const response = await apiClient.post<ApiResponse<User>>('/api/users', accountData)
  return response.data.data
}

// 更新账号
export const updateUser = async (accountData: UpdateUserRequest): Promise<User> => {
  const response = await apiClient.put<ApiResponse<User>>(`/api/users/${accountData.id}`, accountData)
  return response.data.data
}

// 删除账号
export const deleteUser = async (id: number): Promise<void> => {
  await apiClient.delete(`/api/users/${id}`)
}

// 批量删除账号
export const deleteUsers = async (ids: number[]): Promise<void> => {
  await apiClient.post('/api/users/batch-delete', { ids })
}

// 重置账号密码
export const resetUserPassword = async (id: number, newPassword: string): Promise<void> => {
  await apiClient.post(`/api/users/${id}/reset-password`, { password: newPassword })
}

// 切换账号状态
export const toggleUserEnableFlag = async (id: number, enableFlag: boolean): Promise<User> => {
  const response = await apiClient.patch<ApiResponse<User>>(`/api/users/${id}/enableFlag`, { enableFlag })
  return response.data.data
}

// 获取权限列表
export const getPermissions = async (): Promise<Permission[]> => {
  const response = await apiClient.get<ApiResponse<Permission[]>>('/api/permissions')
  return response.data.data
}

// 更新账号权限
export const updateUserPermissions = async (id: number, permissions: Record<string, boolean>): Promise<User> => {
  const response = await apiClient.patch<ApiResponse<User>>(`/api/users/${id}/permissions`, { permissions })
  return response.data.data
}

// 搜索账号
export const searchUsers = async (query: string, filters?: { role?: string, status?: string }, page: number = 1, pageSize: number = 10): Promise<PageResult<User>> => {
  const params = new URLSearchParams({ q: query, page: String(page), pageSize: String(pageSize) })
  if (filters?.role) params.append('role', filters.role)
  if (filters?.status) params.append('status', filters.status)
  const response = await apiClient.get<ApiResponse<PageResult<User>>>(`/api/users/search?${params.toString()}`)
  return response.data.data
}