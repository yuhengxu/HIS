<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { oaApi, type ProcessDefinition, type ProcessNode } from '../../api/oa'

const processes = ref<ProcessDefinition[]>([])
const dialogVisible = ref(false)
const nodeDialogVisible = ref(false)
const editingId = ref<number | null>(null)
const currentProcessId = ref<number | null>(null)
const form = reactive({ code: '', name: '', businessType: '', description: '' })
const nodes = ref<ProcessNode[]>([])
const draggingIndex = ref<number | null>(null)

async function load() {
  processes.value = await oaApi.processes()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { code: '', name: '', businessType: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: ProcessDefinition) {
  editingId.value = row.id
  Object.assign(form, { code: row.code, name: row.name, businessType: row.businessType ?? '', description: row.description ?? '' })
  dialogVisible.value = true
}

async function submit() {
  if (!form.code || !form.name) {
    ElMessage.warning('请填写流程编码和名称')
    return
  }
  if (editingId.value) {
    await oaApi.updateProcess(editingId.value, form)
    ElMessage.success('流程已更新')
  } else {
    await oaApi.createProcess(form)
    ElMessage.success('流程已创建')
  }
  dialogVisible.value = false
  await load()
}

async function remove(row: ProcessDefinition) {
  await ElMessageBox.confirm(`确认删除/停用流程 ${row.name}？`, '提示', { type: 'warning' })
  await oaApi.deleteProcess(row.id)
  ElMessage.success('流程已删除/停用')
  await load()
}

async function publish(row: ProcessDefinition) {
  await oaApi.publishProcess(row.id)
  ElMessage.success('流程已发布')
  await load()
}

async function openNodes(row: ProcessDefinition) {
  currentProcessId.value = row.id
  const detail = await oaApi.getProcess(row.id)
  nodes.value = (detail.nodes ?? []).map((n: ProcessNode) => ({ ...n }))
  if (nodes.value.length === 0) {
    nodes.value.push(defaultNode(10))
    nodes.value.push(draftConfirmNode(20))
  }
  nodeDialogVisible.value = true
}

function defaultNode(sortOrder: number): ProcessNode {
  return {
    nodeCode: `node_${sortOrder}`,
    nodeName: '审批节点',
    nodeType: 'APPROVAL',
    sortOrder,
    assigneeMode: 'ROLE',
    assigneeRoleCode: 'INVENTORY_ADMIN',
    approvePolicy: 'ANY_ONE',
    rejectPolicy: 'BACK_TO_START',
  }
}

function draftConfirmNode(sortOrder: number): ProcessNode {
  return {
    nodeCode: 'draft_confirm',
    nodeName: '起草人确认',
    nodeType: 'APPROVAL',
    sortOrder,
    assigneeMode: 'INITIATOR_SELECTED',
    approvePolicy: 'ANY_ONE',
    rejectPolicy: 'END',
  }
}

function addNode() {
  nodes.value.push(defaultNode((nodes.value.length + 1) * 10))
}

function removeNode(index: number) {
  nodes.value.splice(index, 1)
  normalizeNodeOrder()
}

function statusText(status: string) {
  const map: Record<string, string> = { draft: '草稿', enabled: '已启用', disabled: '已停用', archived: '已归档' }
  return map[status] ?? status
}

async function saveNodes() {
  if (!currentProcessId.value) return
  normalizeNodeOrder()
  await oaApi.saveNodes(currentProcessId.value, nodes.value)
  ElMessage.success('节点已保存')
  nodeDialogVisible.value = false
  await load()
}

function normalizeNodeOrder() {
  nodes.value.forEach((node, index) => {
    node.sortOrder = (index + 1) * 10
  })
}

function dragStart(index: number) {
  draggingIndex.value = index
}

function dragOver(index: number) {
  if (draggingIndex.value === null || draggingIndex.value === index) return
  const [dragging] = nodes.value.splice(draggingIndex.value, 1)
  nodes.value.splice(index, 0, dragging)
  draggingIndex.value = index
  normalizeNodeOrder()
}

function dragEnd() {
  draggingIndex.value = null
  normalizeNodeOrder()
}

function assigneeModeText(mode: string) {
  const map: Record<string, string> = {
    ROLE: '指定角色',
    USER: '指定用户',
    SUPERVISOR: '汇报上级',
    INITIATOR_SELECTED: '起草人确认',
  }
  return map[mode] ?? mode
}

onMounted(load)
</script>

<template>
  <section class="page-section">
    <header class="section-head">
      <h1>OA 流程定义</h1>
      <el-button type="primary" @click="openCreate">新增流程</el-button>
    </header>
    <el-table :data="processes" border>
      <el-table-column prop="code" label="流程编码" width="180" />
      <el-table-column prop="name" label="流程名称" />
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="内置" width="80"><template #default="s">{{ s.row.builtin ? '是' : '否' }}</template></el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link @click="openNodes(scope.row)">节点</el-button>
          <el-button link @click="publish(scope.row)">发布</el-button>
          <el-button link type="danger" :disabled="scope.row.builtin" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑流程' : '新增流程'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="编码"><el-input v-model="form.code" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="业务类型"><el-input v-model="form.businessType" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="nodeDialogVisible" title="审批节点配置" width="900px">
      <el-button type="primary" @click="addNode">新增节点</el-button>
      <div class="node-editor">
        <div class="node-editor-head">
          <span>排序</span>
          <span>节点名称</span>
          <span>节点编码</span>
          <span>处理人模式</span>
          <span>角色/用户</span>
          <span>操作</span>
        </div>
        <div
          v-for="(node, index) in nodes"
          :key="`${node.nodeCode}-${index}`"
          class="node-editor-row"
          :class="{ dragging: draggingIndex === index }"
          draggable="true"
          @dragstart="dragStart(index)"
          @dragover.prevent="dragOver(index)"
          @dragend="dragEnd"
        >
          <div class="drag-handle">拖拽</div>
          <el-input v-model="node.nodeName" />
          <el-input v-model="node.nodeCode" />
          <el-select v-model="node.assigneeMode">
              <el-option label="指定角色" value="ROLE" />
              <el-option label="指定用户" value="USER" />
              <el-option label="汇报上级" value="SUPERVISOR" />
              <el-option label="起草人确认" value="INITIATOR_SELECTED" />
          </el-select>
          <el-input v-if="node.assigneeMode === 'ROLE'" v-model="node.assigneeRoleCode" placeholder="角色编码" />
          <el-input-number v-else-if="node.assigneeMode === 'USER'" v-model="node.assigneeUserId" :min="1" class="form-control" />
          <span v-else class="node-mode-text">{{ assigneeModeText(node.assigneeMode) }}</span>
          <el-button link type="danger" @click="removeNode(index)">删除</el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNodes">保存节点</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.node-editor { margin-top: 12px; display: grid; gap: 8px; }
.node-editor-head,
.node-editor-row {
  display: grid;
  grid-template-columns: 70px 1.1fr 1.1fr 150px 160px 60px;
  gap: 8px;
  align-items: center;
}
.node-editor-head { color: #64748b; font-size: 13px; padding: 0 8px; }
.node-editor-row { padding: 8px; border: 1px solid #e5e7eb; border-radius: 6px; background: #fff; }
.node-editor-row.dragging { opacity: 0.55; border-color: #409eff; }
.drag-handle { cursor: grab; color: #2563eb; user-select: none; }
.node-mode-text { color: #64748b; }
</style>
