<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- 页面标题和操作按钮 -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">客户端管理</h1>
        <p class="text-gray-600 mt-1">管理代理客户端配置和状态</p>
      </div>
      <button
        @click="showAddModal = true"
        class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
        </svg>
        <span>新增客户端</span>
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-lg shadow mb-6 p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-between space-y-4 md:space-y-0">
        <div class="flex-1 max-w-md">
          <div class="relative">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索客户端名称或Token..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
            <svg class="absolute left-3 top-2.5 h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
        </div>
        <div class="flex space-x-4">
          <select v-model="statusFilter" class="border border-gray-300 rounded-lg px-3 py-2">
            <option value="">全部状态</option>
            <option value="online">在线</option>
            <option value="offline">离线</option>
          </select>
          <select v-model="enableFilter" class="border border-gray-300 rounded-lg px-3 py-2">
            <option value="">全部</option>
            <option value="true">已启用</option>
            <option value="false">已禁用</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 客户端列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">客户端信息</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">流量使用</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">代理规则</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="client in filteredClients" :key="client.token" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div class="flex-shrink-0 h-10 w-10">
                    <div class="h-10 w-10 rounded-full bg-indigo-100 flex items-center justify-center">
                      <svg class="h-6 w-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                      </svg>
                    </div>
                  </div>
                  <div class="ml-4">
                    <div class="text-sm font-medium text-gray-900">{{ client.name }}</div>
                    <div class="text-sm text-gray-500">{{ client.token.substring(0, 16) }}...</div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex flex-col space-y-1">
                  <span :class="[
                    'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                    client.status === 'online' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  ]">
                    {{ client.status === 'online' ? '在线' : '离线' }}
                  </span>
                  <span :class="[
                    'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                    client.enableFlag ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'
                  ]">
                    {{ client.enableFlag ? '已启用' : '已禁用' }}
                  </span>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ formatBytes(client.usedTraffic || 0) }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">
                  {{ client.proxyRules?.length || 0 }} 个规则
                </div>
                <button
                  @click="showRulesModal(client)"
                  class="text-indigo-600 hover:text-indigo-900 text-sm"
                >
                  查看详情
                </button>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex space-x-2">
                  <button
                    @click="editClient(client)"
                    class="text-indigo-600 hover:text-indigo-900"
                  >
                    编辑
                  </button>
                  <button
                    @click="toggleClientStatus(client)"
                    :class="client.enableFlag ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'"
                  >
                    {{ client.enableFlag ? '禁用' : '启用' }}
                  </button>
                  <button
                    @click="deleteClient(client)"
                    class="text-red-600 hover:text-red-900"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/编辑客户端模态框 -->
    <div v-if="showAddModal || showEditModal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div class="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
        <div class="mt-3">
          <h3 class="text-lg font-medium text-gray-900 mb-4">
            {{ showAddModal ? '新增客户端' : '编辑客户端' }}
          </h3>
          <form @submit.prevent="saveClient">
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">客户端名称</label>
              <input
                v-model="currentClient.name"
                type="text"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="请输入客户端名称"
              >
            </div>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">Token</label>
              <input
                v-model="currentClient.token"
                type="text"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
                placeholder="请输入Token"
              >
            </div>
            <div class="mb-4">
              <label class="flex items-center">
                <input
                  v-model="currentClient.enableFlag"
                  type="checkbox"
                  class="rounded border-gray-300 text-indigo-600 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50"
                >
                <span class="ml-2 text-sm text-gray-700">启用客户端</span>
              </label>
            </div>
            <div class="flex justify-end space-x-3">
              <button
                type="button"
                @click="closeModal"
                class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300"
              >
                取消
              </button>
              <button
                type="submit"
                class="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700"
              >
                {{ showAddModal ? '新增' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 代理规则模态框 -->
    <div v-if="showRulesModalFlag" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div class="relative top-20 mx-auto p-5 border w-2/3 max-w-4xl shadow-lg rounded-md bg-white">
        <div class="mt-3">
          <div class="flex justify-between items-center mb-4">
            <h3 class="text-lg font-medium text-gray-900">
              {{ selectedClient?.name }} - 代理规则管理
            </h3>
            <button
              @click="addProxyRule"
              class="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1 rounded text-sm"
            >
              新增规则
            </button>
          </div>
          <div class="space-y-3">
            <div v-for="(rule, index) in selectedClient?.proxyRules" :key="index" class="border border-gray-200 rounded-lg p-4">
              <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">规则名称</label>
                  <input
                    v-model="rule.name"
                    type="text"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                    placeholder="规则名称"
                  >
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">服务端口</label>
                  <input
                    v-model="rule.serverPort"
                    type="number"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                    placeholder="8080"
                  >
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-1">客户端地址</label>
                  <input
                    v-model="rule.clientAddress"
                    type="text"
                    class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                    placeholder="localhost:3000"
                  >
                </div>
                <div class="flex items-end space-x-2">
                  <label class="flex items-center">
                    <input
                      v-model="rule.enableFlag"
                      type="checkbox"
                      class="rounded border-gray-300 text-indigo-600"
                    >
                    <span class="ml-1 text-sm text-gray-700">启用</span>
                  </label>
                  <button
                    @click="removeProxyRule(index)"
                    class="text-red-600 hover:text-red-900 text-sm"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end space-x-3 mt-6">
            <button
              @click="closeRulesModal"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300"
            >
              关闭
            </button>
            <button
              @click="saveProxyRules"
              class="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700"
            >
              保存规则
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { ProxyClientConfig, ProxyRule } from '../api/types'

// 响应式数据
const clients = ref<ProxyClientConfig[]>([])
const searchQuery = ref('')
const statusFilter = ref('')
const enableFilter = ref('')
const showAddModal = ref(false)
const showEditModal = ref(false)
const showRulesModalFlag = ref(false)
const selectedClient = ref<ProxyClientConfig | null>(null)
const currentClient = ref<ProxyClientConfig>({
  name: '',
  token: '',
  enableFlag: true,
  proxyRules: []
})

// 计算属性
const filteredClients = computed(() => {
  return clients.value.filter(client => {
    const matchesSearch = !searchQuery.value || 
      client.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      client.token.toLowerCase().includes(searchQuery.value.toLowerCase())
    
    const matchesStatus = !statusFilter.value || client.status === statusFilter.value
    const matchesEnable = !enableFilter.value || client.enableFlag.toString() === enableFilter.value
    
    return matchesSearch && matchesStatus && matchesEnable
  })
})

// 工具函数
const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 客户端操作
const editClient = (client: ProxyClientConfig) => {
  currentClient.value = { ...client }
  showEditModal.value = true
}

const deleteClient = async (client: ProxyClientConfig) => {
  if (confirm(`确定要删除客户端 "${client.name}" 吗？`)) {
    try {
      // 这里应该调用删除API
      const index = clients.value.findIndex(c => c.token === client.token)
      if (index > -1) {
        clients.value.splice(index, 1)
      }
      alert('删除成功')
    } catch (error) {
      console.error('删除客户端失败:', error)
      alert('删除失败')
    }
  }
}

const toggleClientStatus = async (client: ProxyClientConfig) => {
  try {
    const originalStatus = client.enableFlag
    client.enableFlag = !client.enableFlag
    // 这里应该调用更新API
    alert(`客户端已${client.enableFlag ? '启用' : '禁用'}`)
  } catch (error) {
    console.error('更新客户端状态失败:', error)
    client.enableFlag = !client.enableFlag // 回滚
    alert('操作失败')
  }
}

const saveClient = async () => {
  try {
    if (showAddModal.value) {
      // 新增客户端
      const newClient = {
        ...currentClient.value,
        status: 'offline' as const,
        usedTraffic: 0,
        proxyRules: []
      }
      clients.value.push(newClient)
      alert('新增成功')
    } else {
      // 编辑客户端
      const index = clients.value.findIndex(c => c.token === currentClient.value.token)
      if (index > -1) {
        clients.value[index] = { ...currentClient.value }
      }
      alert('保存成功')
    }
    closeModal()
  } catch (error) {
    console.error('保存客户端失败:', error)
    alert('保存失败')
  }
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  currentClient.value = {
    name: '',
    token: '',
    enableFlag: true,
    proxyRules: []
  }
}

// 代理规则操作
const showRulesModal = (client: ProxyClientConfig) => {
  selectedClient.value = { ...client }
  if (!selectedClient.value.proxyRules) {
    selectedClient.value.proxyRules = []
  }
  showRulesModalFlag.value = true
}

const addProxyRule = () => {
  if (selectedClient.value) {
    if (!selectedClient.value.proxyRules) {
      selectedClient.value.proxyRules = []
    }
    selectedClient.value.proxyRules.push({
      name: '',
      serverPort: undefined,
      clientAddress: '',
      enableFlag: true
    })
  }
}

const removeProxyRule = (index: number) => {
  if (selectedClient.value?.proxyRules) {
    selectedClient.value.proxyRules.splice(index, 1)
  }
}

const saveProxyRules = async () => {
  try {
    if (selectedClient.value) {
      const index = clients.value.findIndex(c => c.token === selectedClient.value!.token)
      if (index > -1) {
        clients.value[index].proxyRules = selectedClient.value.proxyRules
      }
    }
    alert('保存成功')
    closeRulesModal()
  } catch (error) {
    console.error('保存代理规则失败:', error)
    alert('保存失败')
  }
}

const closeRulesModal = () => {
  showRulesModalFlag.value = false
  selectedClient.value = null
}

// 加载数据
const loadClients = async () => {
  try {
    // 模拟数据
    clients.value = [
      {
        name: '客户端-001',
        token: 'abc123def456ghi789jkl012mno345pqr678stu901vwx234yz',
        status: 'online',
        usedTraffic: 1024 * 1024 * 500,
        enableFlag: true,
        proxyRules: [
          { name: 'Web服务', serverPort: 8080, clientAddress: 'localhost:3000', enableFlag: true },
          { name: 'API服务', serverPort: 8081, clientAddress: 'localhost:3001', enableFlag: true }
        ]
      },
      {
        name: '客户端-002',
        token: 'def456ghi789jkl012mno345pqr678stu901vwx234yz567abc',
        status: 'offline',
        usedTraffic: 1024 * 1024 * 320,
        enableFlag: false,
        proxyRules: [
          { name: 'SSH隧道', serverPort: 2222, clientAddress: 'localhost:22', enableFlag: false }
        ]
      },
      {
        name: '客户端-003',
        token: 'ghi789jkl012mno345pqr678stu901vwx234yz567abc123def',
        status: 'online',
        usedTraffic: 1024 * 1024 * 280,
        enableFlag: true,
        proxyRules: []
      }
    ]
  } catch (error) {
    console.error('加载客户端列表失败:', error)
  }
}

onMounted(() => {
  loadClients()
})
</script>