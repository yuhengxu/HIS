import { request } from './http'

export type MobileLoginResult = {
  token: string
  userId: number
  username: string
  displayName: string
  roleCodes: string[]
  wecomUserId: string
}

export const wecomApi = {
  authUrl: (redirect = '/m/oa') => request<{ url: string }>(`/api/v1/wecom/auth/url?redirect=${encodeURIComponent(redirect)}`),
  login: (code: string) => request<MobileLoginResult>('/api/v1/wecom/auth/login', { method: 'POST', body: JSON.stringify({ code }) }),
  me: () => request<Record<string, unknown> | null>('/api/v1/wecom/auth/me'),
  logout: () => request<void>('/api/v1/wecom/auth/logout', { method: 'POST' }),
}

export function saveMobileSession(user: MobileLoginResult) {
  localStorage.setItem('his.mobileToken', user.token)
  localStorage.setItem('his.currentUserId', String(user.userId))
  localStorage.setItem('his.currentUser', JSON.stringify({
    id: user.userId,
    username: user.username,
    displayName: user.displayName,
    roleCodes: user.roleCodes,
    wecomUserId: user.wecomUserId,
  }))
}
