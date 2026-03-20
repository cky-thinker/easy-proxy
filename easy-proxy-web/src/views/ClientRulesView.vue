<template>
  <div class="p-6 flex-1">
    <!-- 页面标题和操作按钮 -->
    <div class="relative mb-6 h-24 flex items-center overflow-hidden">
      <div class="z-10">
        <h1 class="text-2xl font-bold text-gray-900">规则管理</h1>
        <p class="text-gray-600 mt-1">管理转发规则</p>
      </div>
      <PageIllustration type="rule" class="absolute right-0 md:right-auto md:left-64 top-1/2 -translate-y-1/2 w-16 h-16 md:w-24 md:h-24" />
    </div>

    <!-- 搜索和筛选 -->
    <div class="bg-white rounded-lg mb-6 pt-4 pl-6">
      <el-form inline label-position="left">
        <el-form-item label="客户端">
          <el-select v-model="queryForm.clientFilter" clearable placeholder="全部" class="w-48">
            <el-option label="全部" :value="undefined" />
            <el-option v-for="c in clients" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="queryForm.nameQuery" clearable placeholder="规则名称">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="queryForm.portQuery" clearable placeholder="端口" class="w-40" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.enableFlag" clearable placeholder="全部" class="w-24">
            <el-option label="全部" :value="undefined" />
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon class="mr-1">
              <Search />
            </el-icon>
            查询
          </el-button>
          <el-button type="success" @click="openAddRuleModal" class="!ml-8">
            <el-icon class="mr-1">
              <Plus />
            </el-icon>
            新增
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 规则列表 -->
    <div class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto p-6">
        <el-table :data="rules" v-loading="loading" :empty-text="'暂无符合条件的规则'">
          <el-table-column label="客户端" width="200">
            <template #default="{ row }">
              {{ clientNameMap[(row as any).proxyClientId || 0] || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="name" label="规则名称" width="300" />
          <el-table-column prop="serverPort" label="服务端口" width="140" />
          <el-table-column prop="clientAddress" label="转发地址" min-width="320" />
          <el-table-column label="启用状态" width="140">
            <template #default="{ row }">
              <el-switch
                v-model="row.enableFlag"
                :loading="(row as any).statusLoading"
                @change="(val: any) => handleEnableChange(row, val as boolean)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" align="center">
            <template #default="{ row }">
              <el-button type="primary" text @click="openDetails(row)">流量分析</el-button>
              <el-button type="primary" text @click="openEditRuleModal(row)">编辑</el-button>
              <el-popconfirm title="确认删除该规则？" @confirm="deleteRuleAction(row)">
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
    <el-pagination class="mt-4 flex justify-end" background :current-page="currentPage" :page-size="pageSize"
      :total="total" layout="prev, pager, next, total" @current-change="onCurrentPageChange" />
    <!-- 新增规则模态框（使用 Element Plus el-dialog 替代） -->
    <el-dialog v-model="showAddRuleModal" title="新增规则" width="600px" :close-on-click-modal="false"
      @close="closeAddRuleModal">
      <el-form :model="newRule" :rules="ruleFormRules" ref="addRuleFormRef" label-position="top">
        <el-form-item label="客户端" prop="proxyClientId">
          <el-select v-model="newRule.proxyClientId" filterable placeholder="请选择客户端" class="w-full">
            <el-option label="请选择客户端" :value="undefined" />
            <el-option v-for="c in clients" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="newRule.name" placeholder="规则名称" class="w-full" />
        </el-form-item>
        <el-form-item label="服务端口" prop="serverPort">
          <el-input v-model="newRule.serverPort" placeholder="服务端口" class="w-full" />
        </el-form-item>
        <el-form-item label="转发地址" prop="clientAddress">
          <el-input v-model="newRule.clientAddress" placeholder="localhost:3000" class="w-full" />
        </el-form-item>
        <el-form-item label="连接数限制" prop="limitConn">
          <el-input v-model.number="newRule.limitConn" placeholder="最大连接数 (0或空不限制)" class="w-full" type="number" />
        </el-form-item>
        <el-form-item label="带宽限制 (KB/s)" prop="limitRate">
          <el-input v-model.number="newRule.limitRate" placeholder="带宽限制 KB/s (0或空不限制)" class="w-full" type="number" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="newRule.enableFlag">启用</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAddRuleModal">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
    <!-- 编辑规则模态框（使用 Element Plus el-dialog 替代） -->
    <el-dialog v-model="showEditRuleModal" title="编辑规则" width="600px" :close-on-click-modal="false"
      @close="closeEditRuleModal">
      <el-form :model="editRule" :rules="ruleFormRulesEdit" ref="editRuleFormRef" label-position="top">
        <el-form-item label="客户端">
          <el-input :model-value="clientNameMap[editRule.proxyClientId || 0] || '-'" disabled class="w-full" />
        </el-form-item>
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="editRule.name" placeholder="规则名称" class="w-full" />
        </el-form-item>
        <el-form-item label="服务端口" prop="serverPort">
          <el-input v-model="editRule.serverPort" class="w-full" />
        </el-form-item>
        <el-form-item label="转发地址" prop="clientAddress">
          <el-input v-model="editRule.clientAddress" placeholder="localhost:3000" class="w-full" />
        </el-form-item>
        <el-form-item label="连接数限制" prop="limitConn">
          <el-input v-model.number="editRule.limitConn" placeholder="最大连接数 (0或空不限制)" class="w-full" type="number" />
        </el-form-item>
        <el-form-item label="带宽限制 (KB/s)" prop="limitRate">
          <el-input v-model.number="editRule.limitRate" placeholder="带宽限制 KB/s (0或空不限制)" class="w-full" type="number" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="editRule.enableFlag">启用</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeEditRuleModal">取消</el-button>
        <el-button type="primary" @click="saveEditRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 规则详情抽屉 -->
    <el-drawer v-model="showDetailsDrawer" title="规则详情" size="600px" @close="closeDetailsDrawer">
      <div class="p-4 space-y-6">
        <!-- 实时流量 -->
        <div class="bg-gray-50 rounded-lg p-4">
          <h3 class="text-lg font-semibold text-gray-900 mb-4">实时流量</h3>
          <div class="grid grid-cols-2 gap-4">
            <div class="bg-white p-4 rounded shadow-sm text-center">
              <div class="text-gray-500 text-sm mb-1">上行速度</div>
              <div class="text-xl font-bold text-blue-600">{{ formatSpeed(realtimeTraffic.uploadSpeed) }}</div>
            </div>
            <div class="bg-white p-4 rounded shadow-sm text-center">
              <div class="text-gray-500 text-sm mb-1">下行速度</div>
              <div class="text-xl font-bold text-green-600">{{ formatSpeed(realtimeTraffic.downloadSpeed) }}</div>
            </div>
          </div>
        </div>
        <!-- 流量趋势 -->
        <div class="h-80">
          <TrafficChart :data="trafficTrend" :loading="trendLoading" @period-change="handlePeriodChange" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import type { ExtendedProxyClientConfig } from '@/api/proxyClient'
import { getAllClients } from '@/api/proxyClient'
import { addClientRule, getClientRulesPage, updateClientRule, getRuleRealtimeTraffic, getRuleTrafficTrend, deleteClientRule } from '@/api/proxyClientRule'
import type { ProxyRule, RuleRealtimeTraffic, TrafficTrend } from '@/api/types'


import TrafficChart from '@/components/TrafficChart.vue'
import PageIllustration from '@/components/PageIllustration.vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

const rules = ref<ProxyRule[]>([])
const clients = ref<ExtendedProxyClientConfig[]>([])
const clientNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  for (const c of clients.value) { map[c.id] = c.name }
  return map
})

const loading = ref(false)
const route = useRoute()
// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPage = ref(1)
const queryForm = reactive<{ nameQuery: string; portQuery?: number; clientFilter?: number; enableFlag?: boolean }>({
  nameQuery: '',
  portQuery: undefined,
  clientFilter: undefined,
  enableFlag: undefined
})

// 新增规则模态与表单
const showAddRuleModal = ref(false)
const newRule = ref<{ name: string; serverPort?: number; clientAddress: string; enableFlag: boolean; proxyClientId?: number; limitConn?: number; limitRate?: number }>({
  name: '',
  serverPort: undefined,
  clientAddress: '',
  enableFlag: true,
  proxyClientId: undefined,
  limitConn: undefined,
  limitRate: undefined
})

const openAddRuleModal = () => {
  resetForm()
  showAddRuleModal.value = true
}

const closeAddRuleModal = () => {
  showAddRuleModal.value = false
}

const showToast = (message: string, type: 'success' | 'error' | 'info' = 'info') => {
  if (type === 'success') ElMessage.success(message)
  else if (type === 'error') ElMessage.error(message)
  else ElMessage.info(message)
}

const resetForm = () => {
  addRuleFormRef.value?.clearValidate?.()
  newRule.value = {
    name: '',
    serverPort: undefined,
    clientAddress: '',
    enableFlag: true,
    proxyClientId: queryForm.clientFilter,
    limitConn: undefined,
    limitRate: undefined
  }
}

const reload = async () => {
  loading.value = true
  try {
    const pageData = await getClientRulesPage({
      page: currentPage.value.toString(),
      pageSize: pageSize.value,
      proxyClientId: queryForm.clientFilter || undefined,
      name: queryForm.nameQuery || undefined,
      serverPort: queryForm.portQuery || undefined,
      enableFlag: queryForm.enableFlag === '' as any ? undefined : queryForm.enableFlag
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
  clients.value = await getAllClients()
  const clientIdParam = Number((route.query.clientId as string) || '')
  if (clientIdParam) {
    queryForm.clientFilter = clientIdParam
  }
  await reload()
})

// 监听筛选变化，自动刷新
const handleQuery = async () => {
  currentPage.value = 1
  await reload()
}

// 分页切换
const onPageChange = async (page: number) => {
  if (page < 1) return
  console.log('onPageChange', page)
  currentPage.value = page
  await reload()
}

// el-pagination 事件（页面为 1 基坐标）
const onCurrentPageChange = async (page: number) => {
  await onPageChange(page)
}

// 编辑规则模态与方法
const showEditRuleModal = ref(false)
const editRule = ref<ProxyRule>({
  id: undefined,
  name: '',
  serverPort: undefined,
  clientAddress: '',
  enableFlag: true,
  proxyClientId: undefined,
  limitConn: undefined,
  limitRate: undefined
})

const openEditRuleModal = (rule: ProxyRule) => {
  editRule.value = {
    id: rule.id,
    name: rule.name,
    serverPort: rule.serverPort,
    clientAddress: rule.clientAddress,
    enableFlag: rule.enableFlag,
    proxyClientId: rule.proxyClientId,
    limitConn: rule.limitConn,
    limitRate: rule.limitRate
  }
  showEditRuleModal.value = true
}

const closeEditRuleModal = () => {
  showEditRuleModal.value = false
}

const addRuleFormRef = ref<FormInstance>()
const editRuleFormRef = ref<FormInstance>()

const ruleFormRules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  serverPort: [
    { required: true, message: '请输入服务端口', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const n = Number(value)
        if (!n || n < 1 || n > 65535) callback(new Error('端口范围为 1-65535'))
        else callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  clientAddress: [{ required: true, message: '请输入客户端地址', trigger: 'blur' }],
  proxyClientId: [
    {
      validator: (_rule, value, callback) => {
        if (value || queryForm.clientFilter) callback()
        else callback(new Error('请选择客户端'))
      },
      trigger: ['change']
    }
  ]
}

const ruleFormRulesEdit: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  serverPort: [
    { required: true, message: '请输入服务端口', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const n = Number(value)
        if (!n || n < 1 || n > 65535) callback(new Error('端口范围为 1-65535'))
        else callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  clientAddress: [{ required: true, message: '请输入客户端地址', trigger: 'blur' }]
}

const saveRule = async () => {
  const valid = await addRuleFormRef.value?.validate?.()
  if (valid !== true) return
  const clientId = newRule.value.proxyClientId ?? queryForm.clientFilter
  if (!clientId) { showToast('请先选择客户端', 'error'); return }
  try {
    const { name, serverPort, clientAddress, enableFlag, limitConn, limitRate } = newRule.value
    await addClientRule(clientId, { name, serverPort: serverPort!, clientAddress, enableFlag, limitConn, limitRate })
    showAddRuleModal.value = false
    await reload()
    showToast('新增成功', 'success')
  } catch (e) {
    console.error('新增规则失败', e)
    showToast('新增失败，请稍后重试', 'error')
  }
}

const saveEditRule = async () => {
  const valid = await editRuleFormRef.value?.validate?.()
  if (valid !== true) return
  const { id, name, serverPort, clientAddress, enableFlag, proxyClientId, limitConn, limitRate } = editRule.value
  if (!id) { showToast('规则ID缺失', 'error'); return }
  try {
    await updateClientRule(proxyClientId || 0, id, { name, serverPort: serverPort!, clientAddress, enableFlag, limitConn, limitRate })
    showEditRuleModal.value = false
    await reload()
    showToast('保存成功', 'success')
  } catch (e) {
    console.error('更新规则失败', e)
    showToast('更新失败，请稍后重试', 'error')
  }
}

const handleEnableChange = async (row: ProxyRule, val: boolean) => {
  try {
    (row as any).statusLoading = true
    await updateClientRule(row.proxyClientId || 0, row.id!, {
      name: row.name,
      serverPort: row.serverPort!,
      clientAddress: row.clientAddress,
      enableFlag: val,
      limitConn: row.limitConn,
      limitRate: row.limitRate
    })
    showToast('状态更新成功', 'success')
  } catch (e) {
    console.error('状态更新失败', e)
    showToast('状态更新失败，请稍后重试', 'error')
    row.enableFlag = !val
  } finally {
    (row as any).statusLoading = false
  }
}

const deleteRuleAction = async (rule: ProxyRule) => {
  try {
    if (!rule.id) return
    // clientId is not strictly needed by the API but required by the function signature
    const clientId = rule.proxyClientId || 0
    await deleteClientRule(clientId, rule.id)
    await reload()
    showToast('删除成功', 'success')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除规则失败:', error)
      showToast('删除失败', 'error')
    }
  }
}

// 详情抽屉相关逻辑
const showDetailsDrawer = ref(false)
const currentRuleId = ref<number | undefined>(undefined)
const realtimeTraffic = ref<RuleRealtimeTraffic>({ uploadSpeed: 0, downloadSpeed: 0 })
const trafficTrend = ref<TrafficTrend[]>([])
const trendLoading = ref(false)
let timer: any = null

const formatSpeed = (bytes: number) => {
  if (!bytes) return '0 B/s'
  const k = 1024
  const sizes = ['B/s', 'KB/s', 'MB/s', 'GB/s', 'TB/s']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const openDetails = (row: ProxyRule) => {
  if (!row.id) return
  currentRuleId.value = row.id
  showDetailsDrawer.value = true
  // Reset data
  realtimeTraffic.value = { uploadSpeed: 0, downloadSpeed: 0 }
  trafficTrend.value = []
  
  fetchRealtimeTraffic()
  fetchTrend('day')
  
  // Start polling
  if (timer) clearInterval(timer)
  timer = setInterval(fetchRealtimeTraffic, 1000)
}

const closeDetailsDrawer = () => {
  showDetailsDrawer.value = false
  if (timer) clearInterval(timer)
  timer = null
  currentRuleId.value = undefined
}

const fetchRealtimeTraffic = async () => {
  if (!currentRuleId.value) return
  try {
    const data = await getRuleRealtimeTraffic(currentRuleId.value)
    realtimeTraffic.value = data
  } catch (e) {
    console.error('获取实时流量失败', e)
  }
}

const handlePeriodChange = (period: 'day' | 'week' | 'month') => {
  fetchTrend(period)
}

const fetchTrend = async (period: 'day' | 'week' | 'month') => {
  if (!currentRuleId.value) return
  trendLoading.value = true
  try {
    const data = await getRuleTrafficTrend(currentRuleId.value, period)
    trafficTrend.value = data
  } catch (e) {
    console.error('获取流量趋势失败', e)
  } finally {
    trendLoading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped></style>
