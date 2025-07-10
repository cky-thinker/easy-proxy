// 代理规则接口
export interface ProxyRule {
  name?: string;
  serverPort?: number;
  clientAddress?: string;
  enableFlag?: boolean;
}

// 代理客户端配置接口
export interface ProxyClientConfig {
  name: string;
  token: string;
  status?: 'online' | 'offline';
  usedTraffic?: number;
  enableFlag?: boolean;
  proxyRules?: ProxyRule[];
}

// 错误响应接口
export interface ErrorResponse {
  error: string;
  path: string;
  status: number;
}

// 健康检查响应接口
export interface HealthResponse {
  status: string;
}