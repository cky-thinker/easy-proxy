import apiClient from '../util/client';
import type { ProxyClientConfig, ApiResponse, PageResult } from './types';

/**
 * 获取所有代理客户端配置
 * @returns Promise<ProxyClientConfig[]>
 */
export const getAllProxyClients = async (): Promise<ProxyClientConfig[]> => {
  const response = await apiClient.get<ProxyClientConfig[]>('/api/proxy-clients');
  return response.data;
};

/**
 * 添加新的代理客户端配置
 * @param proxyClient 代理客户端配置
 * @returns Promise<ProxyClientConfig>
 */
export const addProxyClient = async (proxyClient: ProxyClientConfig): Promise<ProxyClientConfig> => {
  const response = await apiClient.post<ProxyClientConfig>('/api/proxy-clients', proxyClient);
  return response.data;
};

/**
 * 根据token获取代理客户端配置
 * @param token 代理客户端token
 * @returns Promise<ProxyClientConfig>
 */
export const getProxyClientByToken = async (token: string): Promise<ProxyClientConfig> => {
  const response = await apiClient.get<ProxyClientConfig>(`/api/proxy-clients/${token}`);
  return response.data;
};

/**
 * 更新代理客户端配置
 * @param token 代理客户端token
 * @param proxyClient 更新的代理客户端配置
 * @returns Promise<ProxyClientConfig>
 */
export const updateProxyClient = async (token: string, proxyClient: ProxyClientConfig): Promise<ProxyClientConfig> => {
  const response = await apiClient.put<ProxyClientConfig>(`/api/proxy-clients/${token}`, proxyClient);
  return response.data;
};

/**
 * 删除代理客户端配置
 * @param token 代理客户端token
 * @returns Promise<void>
 */
export const deleteProxyClient = async (token: string): Promise<void> => {
  await apiClient.delete(`/api/proxy-clients/${token}`);
};

// 扩展客户端配置接口（从 clients.ts 移动）
export interface ExtendedProxyClientConfig extends ProxyClientConfig {
  id: number;
  status: 'online' | 'offline';
  lastSeen?: string;
  traffic: {
    upload: number;
    download: number;
  };
  connections: number;
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

// 创建客户端
export const createClient = async (
  clientData: Pick<ExtendedProxyClientConfig, 'name' | 'token' | 'enableFlag'>
): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.post<ApiResponse<ExtendedProxyClientConfig>>('/api/proxyClient', clientData)
  return response.data.data
}

// 更新客户端
export const updateClient = async (
  id: number,
  clientData: Partial<ExtendedProxyClientConfig>
): Promise<ExtendedProxyClientConfig> => {
  const response = await apiClient.put<ApiResponse<ExtendedProxyClientConfig>>(`/api/proxyClient`, { id, ...clientData })
  return response.data.data
}

// 删除客户端
export const deleteClient = async (id: number): Promise<void> => {
  await apiClient.delete(`/api/proxyClient`, { params: { id } })
}

// 切换客户端状态（后端未提供单独切换状态端点，使用更新接口）
export const toggleClientStatus = async (
  id: number,
  enableFlag: boolean
): Promise<ExtendedProxyClientConfig> => {
  return updateClient(id, { enableFlag })
}