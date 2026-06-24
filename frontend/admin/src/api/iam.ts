import { request } from './http'

export type UserRecord = {
  id: number
  username: string
  displayName: string
  enabled: boolean
  reportToUserId?: number
  wecomUserId?: string
  departmentName?: string
  roleCodes: string[]
  mustChangePassword?: boolean
}

export type RoleRecord = {
  code: string
  name: string
  description: string
  enabled: boolean
  sortOrder: number
  systemBuiltIn: boolean
  permissionCodes: string[]
}

export type PermissionRecord = {
  code: string
  name: string
  domain: string
  resourceType: string
  description: string
  systemBuiltIn: boolean
  enabled: boolean
  defaultRoleCodes: string[]
}

export type UserRequest = {
  username: string
  displayName: string
  reportToUserId?: number
  wecomUserId?: string
  departmentName?: string
  roleCodes?: string[]
  enabled?: boolean
}

export type RoleRequest = {
  code: string
  name: string
  description: string
  permissionCodes: string[]
  enabled?: boolean
  sortOrder?: number
}

export const iamApi = {
  users: () => request<UserRecord[]>('/api/v1/iam/users'),
  getUser: (userId: number) => request<UserRecord>(`/api/v1/iam/users/${userId}`),
  createUser: (body: UserRequest) => request<UserRecord>('/api/v1/iam/users', { method: 'POST', body: JSON.stringify(body) }),
  updateUser: (userId: number, body: UserRequest) => request<UserRecord>(`/api/v1/iam/users/${userId}`, { method: 'PUT', body: JSON.stringify(body) }),
  updateUserStatus: (userId: number, enabled: boolean) => request<UserRecord>(`/api/v1/iam/users/${userId}/status`, { method: 'PUT', body: JSON.stringify({ enabled }) }),
  assignRoles: (userId: number, roleCodes: string[]) => request<UserRecord>(`/api/v1/iam/users/${userId}/roles`, { method: 'PUT', body: JSON.stringify(roleCodes) }),
  resetPassword: (userId: number) => request<UserRecord>(`/api/v1/iam/users/${userId}/reset-password`, { method: 'POST' }),
  deleteUser: (userId: number) => request<void>(`/api/v1/iam/users/${userId}`, { method: 'DELETE' }),
  roles: () => request<RoleRecord[]>('/api/v1/iam/roles'),
  getRole: (code: string) => request<RoleRecord>(`/api/v1/iam/roles/${code}`),
  createRole: (body: RoleRequest) => request<RoleRecord>('/api/v1/iam/roles', { method: 'POST', body: JSON.stringify(body) }),
  updateRole: (code: string, body: RoleRequest) => request<RoleRecord>(`/api/v1/iam/roles/${code}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteRole: (code: string) => request<void>(`/api/v1/iam/roles/${code}`, { method: 'DELETE' }),
  permissions: () => request<PermissionRecord[]>('/api/v1/iam/permissions'),
  mePermissions: () => request<string[]>('/api/v1/iam/me/permissions'),
  meRoles: () => request<string[]>('/api/v1/iam/me/roles'),
}
