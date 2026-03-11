<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- 使用 Element Plus 消息通知（ElMessage）替代自定义 Toast -->

    <!-- 页面标题 -->
    <div class="relative mb-6 h-24 flex items-center overflow-hidden">
      <div class="z-10">
        <h1 class="text-2xl font-bold text-gray-900">账号管理</h1>
        <p class="text-gray-600 mt-1">管理系统账号和权限</p>
      </div>
      <PageIllustration type="user" class="absolute right-0 md:right-auto md:left-64 top-1/2 -translate-y-1/2 w-48 h-48 md:w-64 md:h-64 opacity-50" />
    </div>

    <!-- 搜索与操作 -->
    <div class="bg-white rounded-lg mb-6 pt-4 pl-6">
      <el-form :model="queryForm" inline label-position="left">
        <el-form-item label="搜索">
          <el-input v-model="queryForm.searchQuery" placeholder="搜索用户名或邮箱..." clearable>
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="queryForm.enableFilter" placeholder="全部" class="w-40">
            <el-option label="全部" value="" />
            <el-option label="激活" value="enabled" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon class="mr-1">
              <Search />
            </el-icon>
            查询
          </el-button>
          <el-button type="success" @click="openAddModal" class="!ml-8">
            <el-icon class="mr-1">
              <Plus />
            </el-icon>
            新增
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 账号列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto p-6">
        <el-table :data="users" v-loading="loading">
          <el-table-column label="用户信息" min-width="320">
            <template #default="{ row }">
              <div class="flex items-center">
                <div class="flex-shrink-0 h-10 w-10">
                  <div class="h-10 w-10 rounded-full bg-indigo-100 flex items-center justify-center">
                    <span class="text-indigo-600 font-medium text-sm">
                      {{ row.username?.charAt(0)?.toUpperCase() || '?' }}
                    </span>
                  </div>
                </div>
                <div class="ml-4">
                  <div class="text-sm font-medium text-gray-900">{{ row.username }}</div>
                  <div class="text-sm text-gray-500">{{ row.email }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="getRoleType(row.role)">{{ getRoleText(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="120">
            <template #default="{ row }">
              <TagEnableFlag :value="row.enableFlag" />
            </template>
          </el-table-column>
          <el-table-column label="最后登录" width="180">
            <template #default="{ row }">
              <span class="text-sm text-gray-900">{{ row.lastLogin || '从未登录' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="350" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text @click="editUser(row)">编辑</el-button>
              <!-- <el-button type="primary" text @click="showPermissionsModal(row)">权限</el-button> -->
              <el-button :type="row.enableFlag ? 'warning' : 'success'" text @click="toggleUserStatus(row)">
                {{ row.enableFlag ? '禁用' : '激活' }}
              </el-button>
              <el-popconfirm title="确认删除该账号？" @confirm="deleteUserAction(row)">
                <template #reference>
                  <el-button type="danger" text>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 分页组件：切换为 Element Plus el-pagination -->

    <el-pagination class="mt-4 flex justify-end" background :current-page="currentPage + 1" :page-size="pageSize"
      :total="total" layout="prev, pager, next, total" @current-change="onCurrentPageChange" />

    <!-- 新增/编辑账号模态框 -->
    <el-dialog
      v-model="showClientModal"
      :title="showAddModal ? '新增账号' : '编辑账号'"
      width="480px"
      :close-on-click-modal="false"
      @close="closeModal"
    >
      <el-form :model="currentUser" :rules="userFormRules" ref="userFormRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="currentUser.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="currentUser.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="showAddModal" label="密码" prop="password">
          <el-input v-model="currentUser.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="currentUser.role" placeholder="请选择角色" class="w-full">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
            <el-option label="只读用户" value="viewer" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeModal">取消</el-button>
        <el-button type="primary" @click="saveUser">{{ showAddModal ? '新增' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <!-- 权限管理模态框 -->
    <el-dialog
      v-model="showPermissionsModalFlag"
      :title="`${selectedUser?.username || ''} - 权限管理`"
      width="800px"
      :close-on-click-modal="false"
      @close="closePermissionsModal"
    >
      <div class="space-y-4">
        <div v-for="(permission, key) in permissions" :key="key" class="border border-gray-200 rounded-lg p-4">
          <div class="flex items-center justify-between mb-2">
            <h4 class="text-md font-medium text-gray-900">{{ permission.name }}</h4>
            <el-checkbox v-model="selectedUser!.permissions[key]">启用</el-checkbox>
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
      <template #footer>
        <el-button @click="closePermissionsModal">取消</el-button>
        <el-button type="primary" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref, watch } from 'vue'
import type { Permission, User } from '../api/types'
import {
  createUser as createUserApi,
  deleteUser as deleteUserApi,
  getUsers as getUsersApi,
  toggleUserEnableFlag as toggleUserEnableFlagApi,
  updateUser as updateUserApi
} from '../api/user'

import TagEnableFlag from '../components/TagEnableFlag.vue'
import PageIllustration from '@/components/PageIllustration.vue'


// 响应式数据
const users = ref<User[]>([])
// 分页与加载状态
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)
const totalPage = ref(1)
const loading = ref(false)
const queryForm = reactive({
  searchQuery: '',
  enableFilter: ''
})
const showAddModal = ref(false)
const showEditModal = ref(false)
const showClientModal = ref(false)
const showPermissionsModalFlag = ref(false)
const selectedUser = ref<User | null>(null)
const currentUser = ref<User>({
  id: 0,
  username: '',
  email: '',
  password: '',
  role: 'user',
  enableFlag: true,
  createdAt: '',
  permissions: {}
})

const userFormRef = ref<FormInstance>()
const userFormRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (showAddModal.value && !value) callback(new Error('请输入密码'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// Toast 提示
const showToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  if (type === 'success') ElMessage.success(message)
  else if (type === 'error') ElMessage.error(message)
  else ElMessage.info(message)
}

const resetForm = () => {
  userFormRef.value?.clearValidate?.()
  currentUser.value = {
    id: 0,
    username: '',
    email: '',
    password: '',
    role: 'user',
    enableFlag: true,
    createdAt: '',
    permissions: {}
  }
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
  users: {
    name: '用户管理',
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

// 使用服务端返回的列表，直接绑定 users 到 el-table

// 工具函数
const getRoleType = (role: string): 'danger' | 'primary' | 'info' => {
  const types = {
    admin: 'danger',
    user: 'primary',
    viewer: 'info'
  } as const
  return types[role as keyof typeof types] || 'info'
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
const openAddModal = () => {
  resetForm()
  showAddModal.value = true
  showEditModal.value = false
  showClientModal.value = true
}

const editUser = (user: User) => {
  currentUser.value = { ...user }
  showEditModal.value = true
  showClientModal.value = true
}

const deleteUserAction = async (user: User) => {
  try {
    await deleteUserApi(user.id)
    const index = users.value.findIndex(a => a.id === user.id)
    if (index > -1) users.value.splice(index, 1)
    showToast('删除成功', 'success')
  } catch (error) {
    console.error('删除账号失败:', error)
    showToast('删除失败', 'error')
  }
}

const toggleUserStatus = async (user: User) => {
  try {
    const targetStatus = !user.enableFlag
    await ElMessageBox.confirm(`确认${targetStatus ? '激活' : '禁用'}账号 \"${user.username}\"？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: targetStatus ? 'info' : 'warning'
    })
    const updated = await toggleUserEnableFlagApi(user.id, targetStatus)
    const index = users.value.findIndex(a => a.id === updated.id)
    if (index > -1) users.value[index] = updated
    showToast(`账号已${updated.enableFlag ? '激活' : '禁用'}`, 'success')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新账号状态失败:', error)
      showToast('操作失败', 'error')
    }
  }
}

const saveUser = async () => {
  try {
    const valid = await userFormRef.value?.validate?.()
    if (valid !== true) return
    if (showAddModal.value) {
      await createUserApi({
        username: currentUser.value.username,
        email: currentUser.value.email,
        password: currentUser.value.password,
        role: currentUser.value.role,
        enableFlag: currentUser.value.enableFlag,
      })
      showToast('新增成功', 'success')
    } else {
      await updateUserApi({
        id: (currentUser.value as any).id as number,
        username: currentUser.value.username,
        email: currentUser.value.email,
        role: currentUser.value.role,
        enableFlag: currentUser.value.enableFlag,
      })
      showToast('保存成功', 'success')
    }
    closeModal()
    await loadUsers()
  } catch (error) {
    console.error('保存账号失败:', error)
    showToast('保存失败', 'error')
  }
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  showClientModal.value = false
  currentUser.value = {
    id: 0,
    username: '',
    email: '',
    password: '',
    role: 'user',
    enableFlag: true,
    createdAt: '',
    permissions: {}
  }
}

// 权限管理
const showPermissionsModal = (user: User) => {
  selectedUser.value = { ...user }
  if (!selectedUser.value.permissions) {
    selectedUser.value.permissions = {}
  }
  showPermissionsModalFlag.value = true
}

const savePermissions = async () => {
  try {
    if (selectedUser.value) {
      const index = users.value.findIndex(a => a.id === selectedUser.value!.id)
      if (index > -1) {
        users.value[index].permissions = selectedUser.value.permissions
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
  selectedUser.value = null
}

// 加载数据

const loadUsers = async () => {
  try {
    loading.value = true
    const hasQueryFilter = !!queryForm.searchQuery
    const hasEnableFlagFilter = queryForm.enableFilter !== ''

    const enableVal =
      hasEnableFlagFilter
        ? queryForm.enableFilter === 'enabled'
          ? true
          : queryForm.enableFilter === 'disabled'
            ? false
            : undefined
        : undefined

    const pageData = await getUsersApi({
      page: currentPage.value,
      pageSize: pageSize.value,
      q: hasQueryFilter ? queryForm.searchQuery : undefined,
      enableFlag: enableVal
    })

    users.value = (pageData.list || [])
    total.value = pageData.total || 0
    totalPage.value = pageData.totalPage || 1
  } catch (error) {
    console.error('加载账号列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  currentPage.value = 0
  await loadUsers()
}

onMounted(() => {
  loadUsers()
})

// 监听筛选条件变化，重置到第一页并调用服务端搜索
watch(
  () => [queryForm.searchQuery, queryForm.enableFilter],
  async () => {
    currentPage.value = 0
    await loadUsers()
  }
)

// 分页处理
const onPageChange = async (page: number) => {
  if (page < 0) return
  currentPage.value = page
  await loadUsers()
}

// el-pagination 事件（页面为 1 基坐标）
const onCurrentPageChange = async (page: number) => {
  await onPageChange(page - 1)
}

// 页大小固定为 10，如后端需要可调整组件支持
</script>
