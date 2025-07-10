import apiClient from './client';
import type { HealthResponse } from './types';

/**
 * 健康检查API
 * @returns Promise<HealthResponse>
 */
export const healthCheck = async (): Promise<HealthResponse> => {
  const response = await apiClient.get<HealthResponse>('/health');
  return response.data;
};