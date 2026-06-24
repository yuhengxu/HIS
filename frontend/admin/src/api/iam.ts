import { request } from './http'

export type UserRecord = {
  id: number
  username: string
  displayName: string
  enabled: boolean
  reportToUserId?: number
  roleCodes: string[]
}

export type RoleRecord = {
  code: string
  name: string
  description: string
  permissionCodes: string[]
}

export type PermissionRecord = {
  code: string
  name: string
  domain: string
}

export const iamApi = {
  users: () => request<UserRecord[]>('/api/v1/iam/users'),
  roles: () => request<RoleRecord[]>('/api/v1/iam/roles'),
  permissions: () => request<PermissionRecord[]>('/api/v1/iam/permissions'),
  mePermissions: () => request<string[]>('/api/v1/iam/me/permissions'),
}
