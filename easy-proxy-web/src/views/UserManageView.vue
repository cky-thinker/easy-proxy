<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- Toast 通知组件 -->
    <div class="fixed top-4 right-4 z-50">
      <div v-if="toast.show" :class="['px-4 py-3 rounded-lg shadow-md transition-all duration-300', 
        toast.type === 'success' ? 'bg-green-500' : 
        toast.type === 'error' ? 'bg-red-500' : 'bg-blue-500']">
        <div class="flex items-center text-white">
          <span>{{ toast.message }}</span>
        </div>
      </div>
    </div>
    
    <!-- 页面标题和操作按钮 -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">账号管理</h1>
        <p class="text-gray-600 mt-1">管理系统用户账号和权限</p>
      </div>
      <button
        @click="() => { showAddModal = true; showClientModal = true; }"
        class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
        </svg>
        <span>新增账号</span>
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
              placeholder="搜索用户名或邮箱..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
            <svg class="absolute left-3 top-2.5 h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
        </div>
        <div class="flex space-x-4">
          <select v-model="roleFilter" class="border border-gray-300 rounded-lg px-3 py-2">
            <option value="">全部角色</option>
            <option value="admin">管理员</option>
            <option value="user">普通用户</option>
            <option value="viewer">只读用户</option>
          </select>
          <select v-model="statusFilter" class="border border-gray-300 rounded-lg px-3 py-2">
            <option value="">全部状态</option>
            <option value="active">激活</option>
            <option value="inactive">禁用</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 账号列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户信息</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">角色</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">最后登录</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">创建时间</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="account in filteredAccounts" :key="account.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div class="flex-shrink-0 h-10 w-10">
                    <div class="h-10 w-10 rounded-full bg-indigo-100 flex items-center justify-center">
                      <span class="text-indigo-600 font-medium text-sm">
                        {{ account.username.charAt(0).toUpperCase() }}
                      </span>
                    </div>
                  </div>
                  <div class="ml-4">
                    <div class="text-sm font-medium text-gray-900">{{ account.username }}</div>
                    <div class="text-sm text-gray-500">{{ account.email }}</div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="[
                  'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                  getRoleColor(account.role)
                ]">
                  {{ getRoleText(account.role) }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span :class="[
                  'inline-flex px-2 py-1 text-xs font-semibold rounded-full',
                  account.status === 'active' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]">
                  {{ account.status === 'active' ? '激活' : '禁用' }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ account.lastLogin || '从未登录' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                {{ account.createdAt }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex space-x-2">
                  <button
                    @click="editAccount(account)"
                    class="text-indigo-600 hover:text-indigo-900"
                  >
                    编辑
                  </button>
                  <button
                    @click="showPermissionsModal(account)"
                    class="text-blue-600 hover:text-blue-900"
                  >
                    权限
                  </button>
                  <button
                    @click="toggleAccountStatus(account)"
                    :class="account.status === 'active' ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'"
                  >
                    {{ account.status === 'active' ? '禁用' : '激活' }}
                  </button>
                  <button
                    @click="deleteAccount(account)"
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

    <!-- 新增/编辑账号模态框 -->
    <Modal v-model="showClientModal" :title="showAddModal ? '新增账号' : '编辑账号'" @confirm="saveAccount" @close="closeModal">
      <form @submit.prevent="saveAccount">
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
          <input
            v-model="currentAccount.username"
            type="text"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            placeholder="请输入用户名"
          >
        </div>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">邮箱</label>
          <input
            v-model="currentAccount.email"
            type="email"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            placeholder="请输入邮箱"
          >
        </div>
        <div class="mb-4" v-if="showAddModal">
          <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
          <input
            v-model="currentAccount.password"
            type="password"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            placeholder="请输入密码"
          >
        </div>
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">角色</label>
          <select
            v-model="currentAccount.role"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="user">普通用户</option>
            <option value="admin">管理员</option>
            <option value="viewer">只读用户</option>
          </select>
        </div>
        <div class="mb-4">
          <label class="flex items-center">
            <input
              v-model="currentAccount.status"
              type="checkbox"
              true-value="active"
              false-value="inactive"
              class="rounded border-gray-300 text-indigo-600 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50"
            >
            <span class="ml-2 text-sm text-gray-700">激活账号</span>
          </label>
        </div>
      </form>
    </Modal>

    <!-- 权限管理模态框 -->
    <Modal v-model="showPermissionsModalFlag" :title="`${selectedAccount?.username} - 权限管理`"  @confirm="savePermissions" @close="closePermissionsModal" :width="'2/3'">
      <div class="space-y-4">
        <div v-for="(permission, key) in permissions" :key="key" class="border border-gray-200 rounded-lg p-4">
          <div class="flex items-center justify-between mb-2">
            <h4 class="text-md font-medium text-gray-900">{{ permission.name }}</h4>
            <label class="flex items-center">
              <input
                v-model="selectedAccount!.permissions[key]"
                type="checkbox"
                class="rounded border-gray-300 text-indigo-600"
              >
              <span class="ml-2 text-sm text-gray-700">启用</span>
            </label>
          </div>
          <p class="text-sm text-gray-600">{{ permission.description }}</p>
          <div class="mt-2">
            <div class="flex flex-wrap gap-2">
              <span v-for="action in permission.actions" :key="action" class="inline-flex px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded">
                {{ action }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { 
  getAccounts, 
  createAccount as apiCreateAccount, 
  updateAccount as apiUpdateAccount, 
  deleteAccount as apiDeleteAccount, 
  toggleAccountStatus as apiToggleAccountStatus
} from '../api/accounts'
import type { Account, Permission, CreateAccountRequest, UpdateAccountRequest } from '../api/types'
import Modal from '../components/Modal.vue'
import Toast from '../components/Toast.vue'

// 响应式数据
const accounts = ref<Account[]>([])
const searchQuery = ref('')
const roleFilter = ref('')
const statusFilter = ref('')
const showAddModal = ref(false)
const showEditModal = ref(false)
const showClientModal = ref(false)
const showPermissionsModalFlag = ref(false)
const selectedAccount = ref<Account | null>(null)
const currentAccount = ref<Account>({
  id: 0,
  username: '',
  email: '',
  password: '',
  role: 'user',
  status: 'active',
  createdAt: '',
  permissions: {}
})

// Toast 提示
const toast = ref({
  show: false,
  message: '',
  type: 'info' as 'success' | 'error' | 'info'
})

// 显示 Toast 提示
const showToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  toast.value = {
    show: true,
    message,
    type
  }
  setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

// 权限配置
const permissions = ref<Record<string, Permission>>({
  dashboard: {
    name: '总览管理',
    description: '查看系统总览和统计信息',
    actions: ['查看', '导出']
  },
  clients: {
    name: '客户端管理',
    description: '管理代理客户端和配置',
    actions: ['查看', '新增', '编辑', '删除', '启用/禁用']
  },
  accounts: {
    name: '账号管理',
    description: '管理系统用户账号',
    actions: ['查看', '新增', '编辑', '删除', '权限管理']
  },
  logs: {
    name: '日志管理',
    description: '查看系统日志和审计记录',
    actions: ['查看', '导出', '清理']
  },
  settings: {
    name: '系统设置',
    description: '管理系统配置和参数',
    actions: ['查看', '修改']
  }
})

// 计算属性
const filteredAccounts = computed(() => {
  return accounts.value.filter(account => {
    const matchesSearch = !searchQuery.value ||
      account.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      account.email.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesRole = !roleFilter.value || account.role === roleFilter.value
    const matchesStatus = !statusFilter.value || account.status === statusFilter.value

    return matchesSearch && matchesRole && matchesStatus
  })
})

// 工具函数
const getRoleColor = (role: string): string => {
  const colors = {
    admin: 'bg-red-100 text-red-800',
    user: 'bg-blue-100 text-blue-800',
    viewer: 'bg-gray-100 text-gray-800'
  }
  return colors[role as keyof typeof colors] || 'bg-gray-100 text-gray-800'
}

const getRoleText = (role: string): string => {
  const texts = {
    admin: '管理员',
    user: '普通用户',
    viewer: '只读用户'
  }
  return texts[role as keyof typeof texts] || '未知角色'
}

// 账号操作
const editAccount = (account: Account) => {
  currentAccount.value = { ...account }
  showEditModal.value = true
  showClientModal.value = true
}

const deleteAccount = async (account: Account) => {
  if (confirm(`确定要删除账号 "${account.username}" 吗？`)) {
    try {
      await apiDeleteAccount(account.id)
      const index = accounts.value.findIndex(a => a.id === account.id)
      if (index > -1) accounts.value.splice(index, 1)
      showToast('删除成功', 'success')
    } catch (error) {
      console.error('删除账号失败:', error)
      showToast('删除失败', 'error')
    }
  }
}

const toggleAccountStatus = async (account: Account) => {
  try {
    const newStatus = account.status === 'active' ? 'inactive' : 'active'
    const updated = await apiToggleAccountStatus(account.id, newStatus)
    // 后端返回的字段映射
    const mapped = mapSysUserToAccount(updated as any)
    const index = accounts.value.findIndex(a => a.id === mapped.id)
    if (index > -1) accounts.value[index] = mapped
    showToast(`账号已${mapped.status === 'active' ? '激活' : '禁用'}`, 'success')
  } catch (error) {
    console.error('更新账号状态失败:', error)
    showToast('操作失败', 'error')
  }
}

const saveAccount = async () => {
  try {
    if (showAddModal.value) {
      // 新增账号
      const payload: CreateAccountRequest = {
        username: currentAccount.value.username,
        email: currentAccount.value.email,
        password: currentAccount.value.password || '',
        role: currentAccount.value.role,
        status: currentAccount.value.status
      }
      const created = await apiCreateAccount(payload)
      const mapped = mapSysUserToAccount(created as any)
      accounts.value.push(mapped)
      showToast('新增成功', 'success')
    } else {
      // 编辑账号
      const payload: UpdateAccountRequest = {
        id: currentAccount.value.id,
        username: currentAccount.value.username,
        email: currentAccount.value.email,
        role: currentAccount.value.role,
        status: currentAccount.value.status
      }
      const updated = await apiUpdateAccount(payload)
      const mapped = mapSysUserToAccount(updated as any)
      const index = accounts.value.findIndex(a => a.id === mapped.id)
      if (index > -1) accounts.value[index] = mapped
      showToast('保存成功', 'success')
    }
    closeModal()
  } catch (error) {
    console.error('保存账号失败:', error)
    showToast('保存失败', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  showClientModal.value = false
  currentAccount.value = {
    id: 0,
    username: '',
    email: '',
    password: '',
    role: 'user',
    status: 'active',
    createdAt: '',
    permissions: {}
  }
}

// 权限管理
const showPermissionsModal = (account: Account) => {
  selectedAccount.value = { ...account }
  if (!selectedAccount.value.permissions) {
    selectedAccount.value.permissions = {}
  }
  showPermissionsModalFlag.value = true
}

const savePermissions = async () => {
  try {
    if (selectedAccount.value) {
      const index = accounts.value.findIndex(a => a.id === selectedAccount.value!.id)
      if (index > -1) {
        accounts.value[index].permissions = selectedAccount.value.permissions
      }
    }
    showToast('权限保存成功', 'success')
    closePermissionsModal()
  } catch (error) {
    console.error('保存权限失败:', error)
    showToast('保存失败', 'error')
  }
}

const closePermissionsModal = () => {
  showPermissionsModalFlag.value = false
  selectedAccount.value = null
}

// 加载数据
const formatDateTime = (value?: string | number | Date): string => {
  if (!value) return ''
  try {
    const d = new Date(value)
    if (isNaN(d.getTime())) return ''
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  } catch { return '' }
}

const mapSysUserToAccount = (raw: any): Account => {
  return {
    id: raw.id,
    username: raw.username,
    email: raw.email,
    role: (raw.role === 'admin' ? 'admin' : raw.role === 'viewer' ? 'viewer' : 'user'),
    status: raw.enableFlag ? 'active' : 'inactive',
    lastLogin: formatDateTime(raw.loginTime),
    createdAt: formatDateTime(raw.createTime) || '',
    permissions: {}
  }
}

const loadAccounts = async () => {
  try {
    const pageData = await getAccounts(0, 10)
    accounts.value = (pageData.list || []).map(item => mapSysUserToAccount(item as any))
  } catch (error) {
    console.error('加载账号列表失败:', error)
  }
}

onMounted(() => {
  loadAccounts()
})
</script>
