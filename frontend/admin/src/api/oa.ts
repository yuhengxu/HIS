import { request } from './http'

export type ProcessDefinition = { id: number; code: string; name: string; version: number; enabled: boolean }
export type OaTask = { id: number; processInstanceId: number; assigneeMode: string; status: string; createdAt: string }
export type ProcessInstance = { id: number; processCode: string; businessType: string; title: string; status: string }

export const oaApi = {
  processes: () => request<ProcessDefinition[]>('/api/v1/oa/process-definitions'),
  instances: () => request<ProcessInstance[]>('/api/v1/oa/instances'),
  todo: () => request<OaTask[]>('/api/v1/oa/tasks/todo'),
  startInbound: (body: unknown) => request<ProcessInstance>('/api/v1/oa/instances/inventory-inbound', { method: 'POST', body: JSON.stringify(body) }),
  startOutbound: (body: unknown) => request<ProcessInstance>('/api/v1/oa/instances/inventory-outbound', { method: 'POST', body: JSON.stringify(body) }),
  startReimbursement: (body: unknown) => request<ProcessInstance>('/api/v1/oa/instances/reimbursement', { method: 'POST', body: JSON.stringify(body) }),
  approve: (taskId: number) => request<ProcessInstance>(`/api/v1/oa/tasks/${taskId}/approve`, { method: 'POST', body: JSON.stringify({ comment: '同意' }) }),
  reject: (taskId: number) => request<ProcessInstance>(`/api/v1/oa/tasks/${taskId}/reject`, { method: 'POST', body: JSON.stringify({ comment: '驳回' }) }),
  urge: (instanceId: number) => request<OaTask>(`/api/v1/oa/instances/${instanceId}/urge`, { method: 'POST' }),
}
