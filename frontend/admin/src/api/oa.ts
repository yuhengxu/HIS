import { request } from './http'

export type ProcessDefinition = {
  id: number
  code: string
  name: string
  version: number
  status: string
  enabled: boolean
  builtin: boolean
  description?: string
  businessType?: string
  nodes?: ProcessNode[]
}

export type ProcessNode = {
  id?: number
  nodeCode: string
  nodeName: string
  nodeType: string
  sortOrder: number
  assigneeMode: string
  assigneeUserId?: number
  assigneeRoleCode?: string
  approvePolicy?: string
  rejectPolicy?: string
}

export type StartableProcess = { processCode: string; businessType: string; title: string; typeCode: string }
export type ProcessInstance = { id: number; processCode: string; businessType: string; title: string; status: string; initiatorUserId?: number }
export type ProcessInstanceView = ProcessInstance & {
  currentTaskId?: number
  currentNodeName: string
  currentHandler: string
  stage: string
}
export type OaTask = { id: number; processInstanceId: number; assigneeMode: string; status: string; createdAt: string; claimedByUserId?: number; handledAt?: string }
export type OaTaskDetail = {
  taskId: number
  processInstanceId: number
  processCode: string
  businessType: string
  title: string
  instanceStatus: string
  initiatorUserId: number
  initiatorName: string
  nodeId: number
  nodeName: string
  assigneeMode: string
  taskStatus: string
  createdAt: string
  formData: Record<string, unknown>
  displayData: Record<string, unknown>
}

export const oaApi = {
  processes: () => request<ProcessDefinition[]>('/api/v1/oa/process-definitions'),
  getProcess: (id: number) => request<ProcessDefinition>(`/api/v1/oa/process-definitions/${id}`),
  createProcess: (body: unknown) => request<ProcessDefinition>('/api/v1/oa/process-definitions', { method: 'POST', body: JSON.stringify(body) }),
  updateProcess: (id: number, body: unknown) => request<ProcessDefinition>(`/api/v1/oa/process-definitions/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteProcess: (id: number) => request<void>(`/api/v1/oa/process-definitions/${id}`, { method: 'DELETE' }),
  publishProcess: (id: number) => request<ProcessDefinition>(`/api/v1/oa/process-definitions/${id}/publish`, { method: 'POST' }),
  copyProcess: (id: number, body: { newCode: string; newName: string }) => request<ProcessDefinition>(`/api/v1/oa/process-definitions/${id}/copy`, { method: 'POST', body: JSON.stringify(body) }),
  saveNodes: (id: number, nodes: ProcessNode[]) => request<ProcessDefinition>(`/api/v1/oa/process-definitions/${id}/nodes`, { method: 'PUT', body: JSON.stringify(nodes) }),
  startable: () => request<StartableProcess[]>('/api/v1/oa/instances/startable'),
  searchMaterials: (keyword?: string) => request<import('./inventory').Item[]>(`/api/v1/oa/instances/materials/search${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),
  searchClaimableMaterials: (warehouseId: number, keyword?: string) =>
    request<import('./inventory').Item[]>(`/api/v1/oa/instances/claimable-materials/search?warehouseId=${warehouseId}${keyword ? `&keyword=${encodeURIComponent(keyword)}` : ''}`),
  instances: () => request<ProcessInstance[]>('/api/v1/oa/instances'),
  myInstances: () => request<ProcessInstanceView[]>('/api/v1/oa/instances/mine'),
  todo: () => request<OaTask[]>('/api/v1/oa/tasks/todo'),
  handled: () => request<OaTask[]>('/api/v1/oa/tasks/handled'),
  taskDetail: (taskId: number) => request<OaTaskDetail>(`/api/v1/oa/tasks/${taskId}`),
  startInbound: (body: unknown) => request<ProcessInstance>('/api/v1/oa/instances/inventory-inbound', { method: 'POST', body: JSON.stringify(body) }),
  startOutbound: (body: unknown) => request<ProcessInstance>('/api/v1/oa/instances/inventory-outbound', { method: 'POST', body: JSON.stringify(body) }),
  startReimbursement: (body: unknown) => request<ProcessInstance>('/api/v1/oa/instances/reimbursement', { method: 'POST', body: JSON.stringify(body) }),
  revoke: (instanceId: number) => request<ProcessInstance>(`/api/v1/oa/instances/${instanceId}/revoke`, { method: 'POST' }),
  approve: (taskId: number) => request<ProcessInstance>(`/api/v1/oa/tasks/${taskId}/approve`, { method: 'POST', body: JSON.stringify({ comment: '同意' }) }),
  reject: (taskId: number) => request<ProcessInstance>(`/api/v1/oa/tasks/${taskId}/reject`, { method: 'POST', body: JSON.stringify({ comment: '驳回' }) }),
  urge: (instanceId: number) => request<OaTask>(`/api/v1/oa/instances/${instanceId}/urge`, { method: 'POST' }),
}
