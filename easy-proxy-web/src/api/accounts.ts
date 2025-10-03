import apiClient from './client'
import type { 
  Account, 
  CreateAccountRequest, 
  UpdateAccountRequest,
  Permission,
  ApiResponse,
  PageResult 
} from './types'

// 获取账号列表（分页）
export const getAccounts = async (page: number = 1, pageSize: number = 10): Promise<PageResult<Account>> => {
  const response = await apiClient.get<ApiResponse<PageResult<Account>>>(`/api/accounts?page=${page}&pageSize=${pageSize}`)
  return response.data.data
}

// 获取单个账号详情
export const getAccount = async (id: number): Promise<Account> => {
  const response = await apiClient.get<ApiResponse<Account>>(`/api/accounts/${id}`)
  return response.data.data
}

// 创建账号
export const createAccount = async (accountData: CreateAccountRequest): Promise<Account> => {
  const response = await apiClient.post<ApiResponse<Account>>('/api/accounts', accountData)
  return response.data.data
}

// 更新账号
export const updateAccount = async (accountData: UpdateAccountRequest): Promise<Account> => {
  const response = await apiClient.put<ApiResponse<Account>>(`/api/accounts/${accountData.id}`, accountData)
  return response.data.data
}

// 删除账号
export const deleteAccount = async (id: number): Promise<void> => {
  await apiClient.delete(`/api/accounts/${id}`)
}

// 批量删除账号
export const deleteAccounts = async (ids: number[]): Promise<void> => {
  await apiClient.post('/api/accounts/batch-delete', { ids })
}

// 重置账号密码
export const resetAccountPassword = async (id: number, newPassword: string): Promise<void> => {
  await apiClient.post(`/api/accounts/${id}/reset-password`, { password: newPassword })
}

// 切换账号状态
export const toggleAccountStatus = async (id: number, status: 'active' | 'inactive'): Promise<Account> => {
  const response = await apiClient.patch<ApiResponse<Account>>(`/api/accounts/${id}/status`, { status })
  return response.data.data
}

// 获取权限列表
export const getPermissions = async (): Promise<Permission[]> => {
  const response = await apiClient.get<ApiResponse<Permission[]>>('/api/permissions')
  return response.data.data
}

// 更新账号权限
export const updateAccountPermissions = async (id: number, permissions: Record<string, boolean>): Promise<Account> => {
  const response = await apiClient.patch<ApiResponse<Account>>(`/api/accounts/${id}/permissions`, { permissions })
  return response.data.data
}

// 搜索账号
export const searchAccounts = async (query: string, filters?: { role?: string, status?: string }, page: number = 1, pageSize: number = 10): Promise<PageResult<Account>> => {
  const params = new URLSearchParams({ q: query, page: String(page), pageSize: String(pageSize) })
  if (filters?.role) params.append('role', filters.role)
  if (filters?.status) params.append('status', filters.status)
  const response = await apiClient.get<ApiResponse<PageResult<Account>>>(`/api/accounts/search?${params.toString()}`)
  return response.data.data
}