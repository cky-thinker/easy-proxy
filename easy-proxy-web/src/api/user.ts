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
export const getUsers = async (params: {
  page?: number,
  pageSize?: number,
  q?: string,
  enableFlag?: boolean
}): Promise<PageResult<User>> => {
  const response = await apiClient.get<ApiResponse<PageResult<User>>>(`/api/users`, {
    params: params
  })
  return response.data.data
}

// 获取单个账号详情
export const getUser = async (id: number): Promise<User> => {
  const response = await apiClient.get<ApiResponse<User>>(`/api/users/detail`, { params: { id } })
  return response.data.data
}

// 创建账号
export const createUser = async (accountData: CreateUserRequest): Promise<User> => {
  const response = await apiClient.post<ApiResponse<User>>('/api/users', accountData)
  return response.data.data
}

// 更新账号
export const updateUser = async (accountData: UpdateUserRequest): Promise<User> => {
  const response = await apiClient.put<ApiResponse<User>>(`/api/users`, accountData)
  return response.data.data
}

// 删除账号
export const deleteUser = async (id: number): Promise<void> => {
  await apiClient.delete(`/api/users`, { params: { id } })
}

// 批量删除账号（后端暂未提供，按需新增）

// 重置账号密码
export const resetUserPassword = async (id: number, newPassword: string): Promise<void> => {
  await apiClient.post(`/api/users/reset-password`, { id, password: newPassword })
}

// 切换账号状态
export const toggleUserEnableFlag = async (id: number, enableFlag: boolean): Promise<User> => {
  const response = await apiClient.put<ApiResponse<User>>(`/api/users/enableFlag`, { id, enableFlag })
  return response.data.data
}

// 获取权限列表
export const getPermissions = async (): Promise<Permission[]> => {
  const response = await apiClient.get<ApiResponse<Permission[]>>('/api/users/permissions')
  return response.data.data
}

// 更新账号权限（后端暂未提供，按需新增）

// 搜索账号（请使用 getUsers 携带 q 与 enableFlag 参数实现）
