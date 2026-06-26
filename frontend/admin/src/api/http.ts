export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

export const currentUserId = () => localStorage.getItem('his.currentUserId')
export const mobileToken = () => localStorage.getItem('his.mobileToken')
export const isLoggedIn = () => Boolean(currentUserId())
export const isMobileLoggedIn = () => Boolean(mobileToken())
export const clearSession = () => {
  localStorage.removeItem('his.currentUserId')
  localStorage.removeItem('his.currentUser')
  localStorage.removeItem('his.mobileToken')
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(mobileToken() ? { Authorization: `Bearer ${mobileToken()}` } : {}),
      ...(!mobileToken() && currentUserId() ? { 'X-User-Id': currentUserId() as string } : {}),
      ...(options.headers ?? {}),
    },
  })
  const body = (await response.json()) as ApiResponse<T>
  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}
