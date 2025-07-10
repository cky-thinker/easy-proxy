<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getAllProxyClients, addProxyClient, healthCheck } from '../api';
import type { ProxyClientConfig } from '../api';

// 状态变量
const proxyClients = ref<ProxyClientConfig[]>([]);
const healthStatus = ref<string>('');
const loading = ref<boolean>(false);
const error = ref<string>('');

// 新客户端表单
const newClient = ref<ProxyClientConfig>({
  name: '',
  token: '',
  enableFlag: true
});

// 加载所有代理客户端
const loadProxyClients = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    proxyClients.value = await getAllProxyClients();
  } catch (err) {
    error.value = '加载代理客户端失败';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

// 检查API健康状态
const checkHealth = async () => {
  try {
    const response = await healthCheck();
    healthStatus.value = response.status;
  } catch (err) {
    healthStatus.value = '不可用';
    console.error(err);
  }
};

// 添加新的代理客户端
const submitNewClient = async () => {
  if (!newClient.value.name || !newClient.value.token) {
    error.value = '名称和Token是必填项';
    return;
  }
  
  loading.value = true;
  error.value = '';
  
  try {
    await addProxyClient(newClient.value);
    // 重置表单
    newClient.value = {
      name: '',
      token: '',
      enableFlag: true
    };
    // 重新加载列表
    await loadProxyClients();
  } catch (err) {
    error.value = '添加代理客户端失败';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadProxyClients();
  checkHealth();
});
</script>

<template>
  <div class="proxy-client-example">
    <h2>代理客户端管理</h2>
    
    <!-- 健康状态 -->
    <div class="health-status">
      API状态: <span :class="{ 'status-up': healthStatus === 'UP' }">{{ healthStatus || '检查中...' }}</span>
    </div>
    
    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
    
    <!-- 添加新客户端表单 -->
    <div class="add-client-form">
      <h3>添加新客户端</h3>
      <div class="form-group">
        <label for="client-name">名称:</label>
        <input id="client-name" v-model="newClient.name" type="text" />
      </div>
      <div class="form-group">
        <label for="client-token">Token:</label>
        <input id="client-token" v-model="newClient.token" type="text" />
      </div>
      <div class="form-group">
        <label for="client-enabled">启用:</label>
        <input id="client-enabled" v-model="newClient.enableFlag" type="checkbox" />
      </div>
      <button @click="submitNewClient" :disabled="loading">添加客户端</button>
    </div>
    
    <!-- 客户端列表 -->
    <div class="client-list">
      <h3>客户端列表</h3>
      <div v-if="loading">加载中...</div>
      <div v-else-if="proxyClients.length === 0">暂无客户端</div>
      <ul v-else>
        <li v-for="client in proxyClients" :key="client.token">
          <div class="client-item">
            <div class="client-name">{{ client.name }}</div>
            <div class="client-token">Token: {{ client.token }}</div>
            <div class="client-status">
              状态: <span :class="{ 'status-online': client.status === 'online' }">
                {{ client.status || 'offline' }}
              </span>
            </div>
            <div class="client-enabled">
              启用: {{ client.enableFlag ? '是' : '否' }}
            </div>
          </div>
        </li>
      </ul>
      <button @click="loadProxyClients" :disabled="loading">刷新列表</button>
    </div>
  </div>
</template>

<style scoped>
.proxy-client-example {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.health-status {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.status-up {
  color: green;
  font-weight: bold;
}

.status-online {
  color: green;
  font-weight: bold;
}

.error-message {
  color: red;
  margin-bottom: 20px;
  padding: 10px;
  background-color: #ffeeee;
  border-radius: 4px;
}

.add-client-form {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

button {
  padding: 8px 16px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  background-color: #cccccc;
}

.client-list {
  margin-top: 20px;
}

ul {
  list-style: none;
  padding: 0;
}

.client-item {
  padding: 15px;
  margin-bottom: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  border-left: 4px solid #4CAF50;
}

.client-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.client-token, .client-status, .client-enabled {
  margin-bottom: 5px;
}
</style>