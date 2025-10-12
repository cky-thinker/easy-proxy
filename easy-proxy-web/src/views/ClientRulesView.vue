<template>
  <div class="p-6 bg-gray-50 min-h-screen">
    <!-- 页面标题和操作按钮 -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">规则管理</h1>
        <p class="text-gray-600 mt-1">管理代理转发规则</p>
      </div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-lg mb-6 p-4">
      <div class="flex flex-col md:flex-row md:items-center md:justify-start space-y-4 md:space-y-0">
        <!-- 搜索输入宽度与 UserManageView 保持一致，整体左对齐 -->
        <div class="max-w-md">
          <div class="relative">
            <input
              v-model="nameQuery"
              type="text"
              placeholder="按规则名称搜索..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
            >
            <svg class="absolute left-3 top-2.5 h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
        </div>
        <div class="flex items-center space-x-4 md:ml-4">
          <input
            v-model.number="portQuery"
            type="number"
            placeholder="按端口筛选，如 8080"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
          >
          <select v-model="clientFilter" class="border border-gray-300 rounded-lg px-3 py-2">
            <option :value="undefined">全部客户端</option>
            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
          <button @click="reload" class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg cursor-pointer">查询</button>
          <button @click="openAddRuleModal" class="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg cursor-pointer">新增规则</button>
        </div>
      </div>
    </div>

    <!-- 规则列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">规则名称</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">服务端端口</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">转发地址</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">客户端</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">启用状态</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr v-for="rule in rules" :key="rule.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ rule.name }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ rule.serverPort }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ rule.clientAddress }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900">{{ clientNameMap[rule.proxyClientId || 0] || '-' }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex space-x-2">
                  <TagEnableFlag :value="rule.enableFlag" />
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex space-x-2">
                  <button @click="openEditRuleModal(rule)" class="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded">编辑</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && rules.length === 0">
              <td colspan="6" class="px-6 py-6 text-center text-gray-500">暂无符合条件的规则</td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="p-4 text-center text-gray-500">加载中...</div>
      </div>
      <!-- 分页组件 -->
      <Pagination
        :currentPage="currentPage"
        :pageSize="pageSize"
        :total="total"
        :totalPage="totalPage"
        :loading="loading"
        @change="onPageChange"
      />
    </div>
  <!-- 新增规则模态框（合并到主模板） -->
  <Modal v-model="showAddRuleModal" title="新增规则" @confirm="saveRule" @close="closeAddRuleModal">
    <form @submit.prevent="saveRule">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">规则名称</label>
          <input v-model="newRule.name" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="规则名称">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">服务端口</label>
          <input v-model.number="newRule.serverPort" type="number" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="8080">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">客户端</label>
          <select v-model="newRule.proxyClientId" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm">
            <option :value="undefined">请选择客户端</option>
            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">客户端地址</label>
          <input v-model="newRule.clientAddress" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="localhost:3000">
        </div>
        <div class="md:col-span-2 flex items-center">
          <label class="flex items-center">
            <input v-model="newRule.enableFlag" type="checkbox" class="rounded border-gray-300 text-indigo-600">
            <span class="ml-2 text-sm text-gray-700">启用</span>
          </label>
        </div>
      </div>
    </form>
  </Modal>
  <!-- 编辑规则模态框 -->
  <Modal v-model="showEditRuleModal" title="编辑规则" @confirm="saveEditRule" @close="closeEditRuleModal">
    <form @submit.prevent="saveEditRule">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">规则名称</label>
          <input v-model="editRule.name" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="规则名称">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">服务端口</label>
          <input v-model.number="editRule.serverPort" type="number" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="8080">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">客户端</label>
          <input :value="clientNameMap[editRule.proxyClientId || 0] || '-'" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm bg-gray-100" disabled>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">客户端地址</label>
          <input v-model="editRule.clientAddress" type="text" class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm" placeholder="localhost:3000">
        </div>
        <div class="md:col-span-2 flex items-center">
          <label class="flex items-center">
            <input v-model="editRule.enableFlag" type="checkbox" class="rounded border-gray-300 text-indigo-600">
            <span class="ml-2 text-sm text-gray-700">启用</span>
          </label>
        </div>
      </div>
    </form>
  </Modal>
</div>
</template>

<script setup lang="ts">
import type { ExtendedProxyClientConfig } from '@/api/clients'
import { addClientRule, getAllClients, getClientRulesPage, updateClientRule } from '@/api/clients'
import type { ProxyRule } from '@/api/types'
import Modal from '@/components/Modal.vue'
import Pagination from '@/components/Pagination.vue'
import TagEnableFlag from '@/components/TagEnableFlag.vue'
import { computed, onMounted, ref, watch } from 'vue'

const rules = ref<ProxyRule[]>([])
const clients = ref<ExtendedProxyClientConfig[]>([])
const clientNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  for (const c of clients.value) { map[c.id] = c.name }
  return map
})

const loading = ref(false)
// 分页状态
const currentPage = ref(0)
const pageSize = ref(10)
const total = ref(0)
const totalPage = ref(1)
const portQuery = ref<number | undefined>(undefined)
const clientFilter = ref<number | undefined>(undefined)
const nameQuery = ref('')

// 新增规则模态与表单
const showAddRuleModal = ref(false)
const newRule = ref<{ name: string; serverPort?: number; clientAddress: string; enableFlag: boolean; proxyClientId?: number }>({
  name: '',
  serverPort: undefined,
  clientAddress: '',
  enableFlag: true,
  proxyClientId: undefined
})

const openAddRuleModal = () => {
  newRule.value = {
    name: '',
    serverPort: undefined,
    clientAddress: '',
    enableFlag: true,
    proxyClientId: clientFilter.value
  }
  showAddRuleModal.value = true
}

const closeAddRuleModal = () => {
  showAddRuleModal.value = false
}

const saveRule = async () => {
  const clientId = newRule.value.proxyClientId ?? clientFilter.value
  const { name, serverPort, clientAddress, enableFlag } = newRule.value
  if (!clientId) { alert('请先选择客户端'); return }
  if (!name || !clientAddress || !serverPort || serverPort <= 0) { alert('请完整填写规则信息'); return }
  try {
    await addClientRule(clientId, { name, serverPort, clientAddress, enableFlag })
    showAddRuleModal.value = false
    await reload()
  } catch (e) {
    console.error('新增规则失败', e)
    alert('新增失败，请稍后重试')
  }
}

const reload = async () => {
  loading.value = true
  try {
    const pageData = await getClientRulesPage({
      page: currentPage.value.toString(),
      pageSize: pageSize.value,
      proxyClientId: clientFilter.value,
      name: nameQuery.value || undefined,
      serverPort: portQuery.value
    })
    rules.value = pageData.list || []
    total.value = pageData.total || 0
    totalPage.value = pageData.totalPage || 1
  } finally {
    loading.value = false
  }
}

// 服务端过滤后直接使用返回列表
const filteredRules = computed(() => rules.value)

onMounted(async () => {
  // 取所有客户端用于下拉选择
  clients.value = await getAllClients()
  await reload()
})

// 监听筛选变化，自动刷新
watch([nameQuery, portQuery, clientFilter], async () => {
  currentPage.value = 0
  await reload()
})

// 分页切换
const onPageChange = async (page: number) => {
  if (page < 0) return
  currentPage.value = page
  await reload()
}

// 编辑规则模态与方法
const showEditRuleModal = ref(false)
const editRule = ref<ProxyRule>({
  id: undefined,
  name: '',
  serverPort: undefined,
  clientAddress: '',
  enableFlag: true,
  proxyClientId: undefined
})

const openEditRuleModal = (rule: ProxyRule) => {
  editRule.value = {
    id: rule.id,
    name: rule.name,
    serverPort: rule.serverPort,
    clientAddress: rule.clientAddress,
    enableFlag: rule.enableFlag,
    proxyClientId: rule.proxyClientId
  }
  showEditRuleModal.value = true
}

const closeEditRuleModal = () => {
  showEditRuleModal.value = false
}

const saveEditRule = async () => {
  const { id, name, serverPort, clientAddress, enableFlag, proxyClientId } = editRule.value
  if (!id) { alert('规则ID缺失'); return }
  if (!name || !clientAddress || !serverPort || serverPort <= 0) { alert('请完整填写规则信息'); return }
  try {
    await updateClientRule(proxyClientId || 0, id, { name, serverPort, clientAddress, enableFlag })
    showEditRuleModal.value = false
    await reload()
  } catch (e) {
    console.error('更新规则失败', e)
    alert('更新失败，请稍后重试')
  }
}
</script>

<style scoped>
</style>