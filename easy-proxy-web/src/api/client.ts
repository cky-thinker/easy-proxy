import axios from 'axios';

// 创建axios实例
const apiClient = axios.create({
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 添加JWT token到请求头
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 统一处理错误
    if (error.response) {
      // 服务器返回错误状态码
      console.error('API错误:', error.response.data);
    } else if (error.request) {
      // 请求发送但没有收到响应
      console.error('网络错误: 没有收到响应');
    } else {
      // 请求设置时发生错误
      console.error('请求错误:', error.message);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
