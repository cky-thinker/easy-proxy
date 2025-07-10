import apiClient from './client';
import type { ProxyClientConfig } from './types';

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