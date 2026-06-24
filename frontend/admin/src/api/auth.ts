import { request } from './http'

export type LoginResponse = {
  userId: number
  username: string
  displayName: string
  roleCodes: string[]
  permissions: string[]
}

export const authApi = {
  login: (body: { username: string; password: string }) =>
    request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(body) }),
}
