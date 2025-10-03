// 代理规则接口
export interface ProxyRule {
  id?: number;
  proxyClientId?: number;
  name?: string;
  serverPort?: number;
  clientAddress?: string;
  enableFlag?: boolean;
}

// 代理客户端配置接口
export interface ProxyClientConfig {
  id?: number;
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

// 登录请求接口
export interface LoginRequest {
  username: string;
  password: string;
  captchaId: string;
  captchaCode: string;
}

// 验证码图片接口
export interface CaptchaImage {
  captchaId: string;
  img: string;
}

// 用户信息接口
export interface UserInfo {
  userId: number;
  username: string;
  avatar?: string;
  token: string;
}

// 仪表板统计接口
export interface DashboardStats {
  onlineClients: number;
  offlineClients: number;
  totalTraffic: number;
  activeConnections: number;
}

// 流量排行接口
export interface TrafficRanking {
  name: string;
  ip: string;
  traffic: number;
  connections: number;
}

// 流量趋势数据接口
export interface TrafficTrend {
  time: string;
  upload: number;
  download: number;
}

// 最近活动接口
export interface RecentActivity {
  message: string;
  time: string;
  type: 'info' | 'warning' | 'error' | 'success';
}

// 账号管理接口
export interface Account {
  id: number;
  username: string;
  email: string;
  role: 'admin' | 'user' | 'viewer';
  status: 'active' | 'inactive';
  lastLogin?: string;
  createdAt: string;
  permissions: Record<string, boolean>;
}

// 权限定义接口
export interface Permission {
  name: string;
  description: string;
  actions: string[];
}

// 创建账号请求接口
export interface CreateAccountRequest {
  username: string;
  email: string;
  password: string;
  role: 'admin' | 'user' | 'viewer';
  status: 'active' | 'inactive';
}

// 更新账号请求接口
export interface UpdateAccountRequest {
  id: number;
  username?: string;
  email?: string;
  role?: 'admin' | 'user' | 'viewer';
  status?: 'active' | 'inactive';
  permissions?: Record<string, boolean>;
}

// API响应接口
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

// 分页响应结构（与服务端 PageResult 对齐）
export interface PageResult<T> {
  page: number;
  pageSize: number;
  totalPage: number;
  total: number;
  list: T[];
}