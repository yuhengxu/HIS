<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { oaApi, type OaTask, type OaTaskDetail, type ProcessInstanceView } from '../api/oa'

const tasks = ref<OaTask[]>([])
const handledTasks = ref<OaTask[]>([])
const instances = ref<ProcessInstanceView[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<OaTaskDetail | null>(null)

async function load() {
  loading.value = true
  try {
    await Promise.all([loadTodos(), loadInstances(), loadHandled()])
  } finally {
    loading.value = false
  }
}

async function loadTodos() {
  try {
    tasks.value = await oaApi.todo()
  } catch (error) {
    tasks.value = []
    ElMessage.warning(`待办加载失败：${error instanceof Error ? error.message : '请稍后重试'}`)
  }
}

async function loadInstances() {
  try {
    instances.value = await oaApi.myInstances()
  } catch (error) {
    instances.value = []
    ElMessage.warning(`我发起的流程加载失败：${error instanceof Error ? error.message : '请稍后重试'}`)
  }
}

async function loadHandled() {
  try {
    handledTasks.value = await oaApi.handled()
  } catch (error) {
    handledTasks.value = []
    ElMessage.warning(`已办记录加载失败：${error instanceof Error ? error.message : '请稍后重试'}`)
  }
}

async function approve(id: number) {
  await oaApi.approve(id)
  ElMessage.success('已审批')
  detailVisible.value = false
  await load()
}

async function reject(id: number) {
  await oaApi.reject(id)
  ElMessage.warning('已驳回')
  detailVisible.value = false
  await load()
}

async function revoke(instanceId: number) {
  await oaApi.revoke(instanceId)
  ElMessage.success('流程已撤销')
  await load()
}

async function openDetail(id: number) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    currentDetail.value = await oaApi.taskDetail(id)
  } catch (error) {
    detailVisible.value = false
    ElMessage.warning(`详情加载失败：${error instanceof Error ? error.message : '请稍后重试'}`)
  } finally {
    detailLoading.value = false
  }
}

function statusText(status: string) {
  const map: Record<string, string> = {
    RUNNING: '审批中',
    APPROVED: '已结束',
    REJECTED: '已驳回',
    CANCELLED: '已撤销',
    PENDING_CONFIG: '待配置',
    PENDING: '待处理',
  }
  return map[status] ?? status
}

function taskStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    APPROVED: '已同意',
    REJECTED: '已驳回',
    CANCELLED: '已取消',
  }
  return map[status] ?? status
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function assigneeModeText(mode: string) {
  const map: Record<string, string> = {
    SUPERVISOR: '汇报上级',
    ROLE: '指定角色',
    USER: '指定用户',
    INITIATOR_SELECTED: '起草人确认',
  }
  return map[mode] ?? mode
}

function formatValue(value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  if (Array.isArray(value)) return value.length ? value.join('、') : '-'
  if (typeof value === 'object') return JSON.stringify(value, null, 2)
  return String(value)
}

function detailRows(detail: OaTaskDetail | null) {
  if (!detail) return []
  const entries = Object.entries(detail.displayData ?? {})
  if (entries.length) return entries
  return Object.entries(detail.formData ?? {})
}

onMounted(load)
</script>

<template>
  <section class="workspace" v-loading="loading">
    <section class="page-section">
      <header class="section-head">
        <h1>待处理事项</h1>
        <el-button @click="load">刷新</el-button>
      </header>
      <el-table :data="tasks" border empty-text="暂无待办">
        <el-table-column prop="id" label="任务 ID" width="100" />
        <el-table-column prop="processInstanceId" label="流程实例" width="110" />
        <el-table-column label="处理模式">
          <template #default="scope">{{ assigneeModeText(scope.row.assigneeMode) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">{{ statusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="scope">
            <el-button size="small" @click="openDetail(scope.row.id)">详情</el-button>
            <el-button size="small" type="primary" @click="approve(scope.row.id)">同意</el-button>
            <el-button size="small" @click="reject(scope.row.id)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="page-section">
      <header class="section-head">
        <h1>我处理过的事项</h1>
      </header>
      <el-table :data="handledTasks" border empty-text="暂无已办记录">
        <el-table-column prop="id" label="任务 ID" width="100" />
        <el-table-column prop="processInstanceId" label="流程实例" width="110" />
        <el-table-column label="处理模式">
          <template #default="scope">{{ assigneeModeText(scope.row.assigneeMode) }}</template>
        </el-table-column>
        <el-table-column label="处理结果" width="120">
          <template #default="scope">{{ taskStatusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="180">
          <template #default="scope">{{ formatTime(scope.row.handledAt) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <section class="page-section">
      <header class="section-head">
        <h1>我发起的流程</h1>
      </header>
      <el-table :data="instances" border empty-text="暂无发起流程">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="流程标题" />
        <el-table-column label="阶段" width="100">
          <template #default="scope">{{ scope.row.stage }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">{{ statusText(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column prop="currentHandler" label="流转到谁" />
        <el-table-column prop="currentNodeName" label="当前节点" />
        <el-table-column label="操作" width="110">
          <template #default="scope">
            <el-button v-if="scope.row.status === 'RUNNING' || scope.row.status === 'PENDING_CONFIG'" link type="danger" @click="revoke(scope.row.id)">撤销</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="detailVisible" title="待办详情" size="520px">
      <section v-loading="detailLoading" class="task-detail">
        <template v-if="currentDetail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="流程">{{ currentDetail.title }}</el-descriptions-item>
            <el-descriptions-item label="发起人">{{ currentDetail.initiatorName }}</el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ currentDetail.nodeName }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ statusText(currentDetail.instanceStatus) }}</el-descriptions-item>
          </el-descriptions>
          <h2>申请内容</h2>
          <el-descriptions :column="1" border>
            <el-descriptions-item v-for="[key, value] in detailRows(currentDetail)" :key="key" :label="key">
              <pre v-if="typeof value === 'object' && value !== null" class="detail-pre">{{ formatValue(value) }}</pre>
              <span v-else>{{ formatValue(value) }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <div class="detail-actions">
            <el-button type="primary" @click="approve(currentDetail.taskId)">同意</el-button>
            <el-button @click="reject(currentDetail.taskId)">驳回</el-button>
          </div>
        </template>
      </section>
    </el-drawer>
  </section>
</template>
