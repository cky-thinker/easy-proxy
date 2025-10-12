<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- 页面标题和操作按钮 -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">客户端管理</h1>
        <p class="text-gray-600 mt-1">管理代理客户端配置和状态</p>
      </div>
      <button
        @click="openAddModal"
        class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2 cursor-pointer"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
        </svg>
        <span>新增客户端</span>
      </button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-lg mb-6 p-4">
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
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">连接状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">启用状态</th>
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
                  <div class="flex space-x-2">
                    <TagStatus :value="client.status" />
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex flex-col space-y-1">
                  <div class="flex space-x-2">
                    <TagEnableFlag :value="client.enableFlag" />
                  </div>
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
                  class="text-indigo-600 hover:text-indigo-900 text-sm cursor-pointer"
                >
                  查看详情
                </button>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex space-x-2">
                  <button
                    @click="editClient(client)"
                    class="text-indigo-600 hover:text-indigo-900 cursor-pointer"
                  >
                    编辑
                  </button>
                  <button
                    @click="toggleStatus(client)"
                    class="cursor-pointer"
                    :class="client.enableFlag ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'"
                  >
                    {{ client.enableFlag ? '禁用' : '启用' }}
                  </button>
                  <button
                    @click="deleteClientAction(client)"
                    class="text-red-600 hover:text-red-900 cursor-pointer"
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
    <Modal 
      v-model="showClientModal" 
      :title="showAddModal ? '新增客户端' : '编辑客户端'"
      width="w-96"
      @cancel="closeModal"
      @confirm="saveClient"
      :confirm-text="showAddModal ? '新增' : '保存'"
    >
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
          <div class="flex items-center space-x-2">
            <input
              v-model="currentClient.token"
              type="text"
              required
              class="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="请输入Token"
            >
            <button
              type="button"
              @click="generateToken"
              class="px-3 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 cursor-pointer border border-gray-200"
              title="生成64位随机Token"
            >
              生成
            </button>
          </div>
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
      </form>
    </Modal>

    <!-- 代理规则模态框 -->
    <Modal 
      v-model="showRulesModalFlag" 
      :title="selectedClient?.name + ' - 代理规则管理'"
      width="w-2/3 max-w-4xl"
      @cancel="closeRulesModal"
      @confirm="saveProxyRules"
      confirm-text="保存规则"
    >
      <div class="flex justify-end mb-4">
        <button
          @click="addProxyRule"
          class="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1 rounded text-sm cursor-pointer"
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
                class="text-red-600 hover:text-red-900 text-sm cursor-pointer"
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </div>
  <!-- 悬浮轻提示（Toast） -->
  <Toast 
    :show="toastVisible" 
    :message="toastMessage" 
    :type="toastType" 
    @update:show="toastVisible = $event" 
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TagStatus from '../components/TagStatus.vue'
import TagEnableFlag from '../components/TagEnableFlag.vue'
import type { ProxyClientConfig, ProxyRule } from '../api/types'
import { 
  getClients, createClient, updateClient, deleteClient as deleteClientApi,
  getClientRules, addClientRule, updateClientRule, deleteClientRule,
  toggleClientStatus as toggleClientStatusApi
} from '../api/clients'
import Toast, { type ToastType } from '../components/Toast.vue'
import Modal from '../components/Modal.vue'

// 响应式数据
const clients = ref<ProxyClientConfig[]>([])
const searchQuery = ref('')
const statusFilter = ref('')
const enableFilter = ref('')
const showAddModal = ref(false)
const showEditModal = ref(false)
const showClientModal = ref(false)
const showRulesModalFlag = ref(false)
const selectedClient = ref<ProxyClientConfig | null>(null)
const currentClient = ref<ProxyClientConfig>({
  name: '',
  token: '',
  enableFlag: true,
  proxyRules: []
})

// 分页与加载状态
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 计算属性
const filteredClients = computed(() => {
  return clients.value.filter((client: ProxyClientConfig) => {
    const matchesSearch = !searchQuery.value || 
      client.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      client.token.toLowerCase().includes(searchQuery.value.toLowerCase())
    
    const matchesStatus = !statusFilter.value || client.status === statusFilter.value
    const matchesEnable = !enableFilter.value || (client?.enableFlag?.toString() === enableFilter.value)
    
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

const deleteClientAction = async (client: ProxyClientConfig) => {
  if (confirm(`确定要删除客户端 "${client.name}" 吗？`)) {
    try {
      await deleteClientApi((client as any).id as number)
      await loadClients()
      showToast('删除成功', 'success')
    } catch (error) {
      console.error('删除客户端失败:', error)
      showToast('删除失败', 'error')
    }
  }
}

const toggleStatus = async (client: ProxyClientConfig) => {
  try {
    await toggleClientStatusApi((client as any).id as number, !client.enableFlag)
    showToast(`客户端已${!client.enableFlag ? '启用' : '禁用'}`, 'success')
    await loadClients()
  } catch (error) {
    console.error('更新客户端状态失败:', error)
    showToast('操作失败', 'error')
  }
}

// 打开新增客户端模态框
const openAddModal = () => {
  showAddModal.value = true
  showEditModal.value = false
  showClientModal.value = true
  currentClient.value = {
    name: '',
    token: '',
    enableFlag: true,
    proxyRules: []
  }
}

// 打开编辑客户端模态框
function editClient(client: ProxyClientConfig) {
  showAddModal.value = false
  showEditModal.value = true
  showClientModal.value = true
  currentClient.value = { ...client }
}

const saveClient = async () => {
  try {
    if (showAddModal.value) {
      await createClient({
        name: currentClient.value.name,
        token: currentClient.value.token,
        enableFlag: currentClient.value.enableFlag
      })
      showToast('新增成功', 'success')
    } else {
      await updateClient((currentClient.value as any).id as number, {
        name: currentClient.value.name,
        token: currentClient.value.token,
        enableFlag: currentClient.value.enableFlag
      })
      showToast('保存成功', 'success')
    }
    closeModal()
    await loadClients()
  } catch (error) {
    console.error('保存客户端失败:', error)
    showToast('保存失败', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  showClientModal.value = false
  currentClient.value = {
    name: '',
    token: '',
    enableFlag: true,
    proxyRules: []
  }
}

// 代理规则操作
const showRulesModal = async (client: ProxyClientConfig) => {
  selectedClient.value = { ...client }
  showRulesModalFlag.value = true
  try {
    const rules = await getClientRules((client as any).id as number)
    selectedClient.value.proxyRules = (rules || []).filter(r => (r as any).proxyClientId === (client as any).id)
  } catch (error) {
    console.error('加载代理规则失败:', error)
  }
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

const removeProxyRule = async (index: number) => {
  if (selectedClient.value?.proxyRules) {
    const rule = selectedClient.value.proxyRules[index]
    try {
      if ((rule as any).id) {
        await deleteClientRule((selectedClient.value as any).id as number, (rule as any).id as number)
      }
      selectedClient.value.proxyRules.splice(index, 1)
    } catch (error) {
      console.error('删除规则失败:', error)
      alert('删除失败')
    }
  }
}

const saveProxyRules = async () => {
  try {
    if (selectedClient.value) {
      const clientId = (selectedClient.value as any).id as number
      for (const rule of selectedClient.value.proxyRules || []) {
        if (!(rule as any).id) {
          const created = await addClientRule(clientId, rule as any)
          Object.assign(rule, created)
        } else {
          const updated = await updateClientRule(clientId, (rule as any).id as number, rule as any)
          Object.assign(rule, updated)
        }
      }
    }
    showToast('保存成功', 'success')
    closeRulesModal()
  } catch (error) {
    console.error('保存代理规则失败:', error)
    showToast('保存失败', 'error')
  }
}

const closeRulesModal = () => {
  showRulesModalFlag.value = false
  selectedClient.value = null
}

// 加载数据
const loadClients = async () => {
  try {
    loading.value = true
    const result = await getClients(currentPage.value, pageSize.value, searchQuery.value || undefined)
    clients.value = result.list || []
    total.value = result.total || 0
  } catch (error) {
    console.error('加载客户端列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadClients()
})

// 生成64位随机字符串（使用32字节的十六进制表示）
const generateToken = () => {
  const bytes = new Uint8Array(32)
  crypto.getRandomValues(bytes)
  currentClient.value.token = Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}

// 轻提示（Toast）
const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref<ToastType>('info')

const showToast = (message: string, type: ToastType = 'info') => {
  toastMessage.value = message
  toastType.value = type
  toastVisible.value = true
}
</script>