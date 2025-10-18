<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- 页面标题 -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">客户端管理</h1>
        <p class="text-gray-600 mt-1">管理代理客户端配置和状态</p>
      </div>
    </div>

    <!-- 搜索与操作 -->
    <div class="bg-white rounded-lg mb-6 p-4">
      <el-form :model="queryForm" inline label-position="left">
        <el-form-item label="搜索">
          <el-input v-model="queryForm.searchQuery" placeholder="名称或Token" clearable>
          </el-input>
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select v-model="queryForm.statusFilter" placeholder="全部" class="w-48">
            <el-option label="全部" value="" />
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="queryForm.enableFilter" placeholder="全部" class="w-48">
            <el-option label="全部" value="" />
            <el-option label="启用" value="enabled" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button type="success" @click="openAddModal">
            <el-icon class="mr-1">
              <Plus />
            </el-icon>
            新增
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 客户端列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">客户端名称</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Token</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">启用</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">规则数</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="client in clients" :key="client.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ client.name }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ client.token }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <TagStatus :value="client.status" />
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <TagEnableFlag :value="client.enableFlag" />
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ client.proxyRules?.length || 0 }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex space-x-2">
                  <button @click="openEditModal(client)" class="text-indigo-600 hover:text-indigo-900">编辑</button>
                  <button @click="openRulesModal(client)" class="text-blue-600 hover:text-blue-900">规则</button>
                  <button
                    @click="toggleClientStatus(client)"
                    :class="client.enableFlag ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'"
                  >
                    {{ client.enableFlag ? '禁用' : '启用' }}
                  </button>
                  <button @click="deleteClientAction(client)" class="text-red-600 hover:text-red-900 cursor-pointer">
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分页组件 -->
    <el-pagination class="mt-4 flex justify-end" background :current-page="currentPage + 1" :page-size="pageSize"
      :total="total" layout="prev, pager, next, total" @current-change="onCurrentPageChange" />

    <!-- 新增/编辑客户端模态框：替换为 Element Plus el-dialog -->
    <el-dialog
      v-model="showClientModal"
      :title="showAddModal ? '新增客户端' : '编辑客户端'"
      width="480px"
      :close-on-click-modal="false"
      @close="closeModal"
    >
      <el-form :model="currentClient" :rules="clientFormRules" ref="clientFormRef" label-position="top">
        <el-form-item label="客户端名称" prop="name">
          <el-input v-model="currentClient.name" placeholder="请输入客户端名称" class="w-full" />
        </el-form-item>
        <el-form-item label="Token" prop="token">
          <div class="flex items-center space-x-2">
            <el-input v-model="currentClient.token" placeholder="请输入Token" class="flex-1" />
            <el-button type="default" @click="generateToken">生成</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="currentClient.enableFlag">启用客户端</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeModal">取消</el-button>
        <el-button type="primary" @click="saveClient">{{ showAddModal ? '新增' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <!-- 代理规则模态框：替换为 Element Plus el-dialog -->
    <el-dialog
      v-model="showRulesModalFlag"
      :title="(selectedClient?.name || '') + ' - 代理规则管理'"
      width="800px"
      :close-on-click-modal="false"
      @close="closeRulesModal"
    >
      <div class="flex justify-end mb-4">
        <el-button type="success" size="small" @click="addProxyRule">
          <el-icon class="mr-1">
            <Plus />
          </el-icon>
          新增
        </el-button>
      </div>
      <div class="space-y-3">
        <div v-for="(rule, index) in selectedClient?.proxyRules" :key="index" class="border border-gray-200 rounded-lg p-4">
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">规则名称</label>
              <el-input v-model="rule.name" placeholder="规则名称" class="w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">服务端口</label>
              <el-input-number v-model="rule.serverPort" :min="1" :max="65535" controls-position="right" class="w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">客户端地址</label>
              <el-input v-model="rule.clientAddress" placeholder="localhost:3000" class="w-full" />
            </div>
            <div class="flex items-end space-x-2">
              <el-checkbox v-model="rule.enableFlag">启用</el-checkbox>
              <el-popconfirm title="确认删除该规则？" @confirm="removeProxyRule(index)">
                <template #reference>
                  <el-button type="danger" text size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeRulesModal">取消</el-button>
        <el-button type="primary" @click="saveProxyRules">保存规则</el-button>
      </template>
    </el-dialog>
  </div>
  <!-- 使用 Element Plus 消息通知（ElMessage）替代自定义 Toast） -->
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref, watch, reactive } from 'vue'
import {
  addClientRule,
  createClient,
  deleteClient as deleteClientApi,
  deleteClientRule,
  getClientRules,
  getClients,
  toggleClientStatus as toggleClientStatusApi,
  updateClient,
  updateClientRule
} from '../api/clients'
import type { ProxyClientConfig } from '../api/types'
// 移除自定义 Modal 引用
// import Modal from '../components/Modal.vue'
import TagEnableFlag from '../components/TagEnableFlag.vue'
import TagStatus from '../components/TagStatus.vue'


// 响应式数据
const clients = ref<ProxyClientConfig[]>([])
const queryForm = reactive({
  searchQuery: '',
  statusFilter: '',
  enableFilter: ''
})
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

const clientFormRef = ref<FormInstance>()
const clientFormRules: FormRules = {
  name: [{ required: true, message: '请输入客户端名称', trigger: 'blur' }],
  token: [
    { required: true, message: '请输入 Token', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback()
        const hex64 = /^[0-9a-fA-F]{64}$/
        if (!hex64.test(value)) callback(new Error('Token 必须为 64 位十六进制字符串'))
        else callback()
      },
      trigger: ['blur', 'change']
    }
  ]
}

// 分页与加载状态
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)
const totalPage = ref(1)
const loading = ref(false)

// 消息提示（ElMessage）
const showToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  if (type === 'success') ElMessage.success(message)
  else if (type === 'error') ElMessage.error(message)
  else ElMessage.info(message)
}

// 客户端操作
const deleteClientAction = async (client: ProxyClientConfig) => {
  try {
    await ElMessageBox.confirm(`确定要删除客户端 \"${client.name}\" 吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteClientApi((client as any).id as number)
    await loadClients()
    showToast('删除成功', 'success')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除客户端失败:', error)
      showToast('删除失败', 'error')
    }
  }
}

const toggleClientStatus = async (client: ProxyClientConfig) => {
  try {
    const targetStatus = !(client as any).enableFlag
    await ElMessageBox.confirm(`确认${targetStatus ? '启用' : '禁用'}客户端 \"${client.name}\"？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: targetStatus ? 'info' : 'warning'
    })
    await toggleClientStatusApi((client as any).id as number, targetStatus)
    await loadClients()
    showToast(`客户端已${targetStatus ? '启用' : '禁用'}`, 'success')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新客户端状态失败:', error)
      showToast('操作失败', 'error')
    }
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
const openEditModal = (client: ProxyClientConfig) => {
  showAddModal.value = false
  showEditModal.value = true
  showClientModal.value = true
  currentClient.value = { ...client }
}

const saveClient = async () => {
  try {
    const valid = await clientFormRef.value?.validate?.()
    if (valid !== true) return
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
const openRulesModal = async (client: ProxyClientConfig) => {
  selectedClient.value = { ...client }
  showRulesModalFlag.value = true
  try {
    const rules = await getClientRules({ proxyClientId: client.id })
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
    } as any)
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
      ElMessage.error('删除失败，请稍后重试')
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
    const hasQuery = !!queryForm.searchQuery
    const hasStatus = !!queryForm.statusFilter
    const hasEnable = queryForm.enableFilter !== ''

    const enableVal =
      hasEnable
        ? queryForm.enableFilter === 'enabled'
          ? true
          : queryForm.enableFilter === 'disabled'
          ? false
          : undefined
        : undefined

    const result = await getClients(
      currentPage.value,
      pageSize.value,
      hasQuery ? queryForm.searchQuery : undefined,
      hasStatus ? (queryForm.statusFilter as 'online' | 'offline') : undefined,
      enableVal
    )
    clients.value = result.list || []
    total.value = result.total || 0
    totalPage.value = result.totalPage || 1
  } catch (error) {
    console.error('加载客户端列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadClients()
})

// 手动查询按钮
const handleQuery = async () => {
  currentPage.value = 0
  await loadClients()
}

// 监听筛选变化，重置到第一页并重新加载
watch(
  () => [queryForm.searchQuery, queryForm.statusFilter, queryForm.enableFilter],
  async () => {
    currentPage.value = 0
    await loadClients()
  }
)

// 分页切换
const onPageChange = async (page: number) => {
  if (page < 0) return
  currentPage.value = page
  await loadClients()
}

// el-pagination 事件（页面为 1 基坐标）
const onCurrentPageChange = async (page: number) => {
  await onPageChange(page - 1)
}

// 生成64位随机字符串（使用32字节的十六进制表示）
const generateToken = () => {
  const bytes = new Uint8Array(32)
  crypto.getRandomValues(bytes)
  currentClient.value.token = Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}
</script>