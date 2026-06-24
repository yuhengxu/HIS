export type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

export const currentUserId = () => localStorage.getItem('his.currentUserId')
export const isLoggedIn = () => Boolean(currentUserId())
export const clearSession = () => {
  localStorage.removeItem('his.currentUserId')
  localStorage.removeItem('his.currentUser')
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(currentUserId() ? { 'X-User-Id': currentUserId() as string } : {}),
      ...(options.headers ?? {}),
    },
  })
  const body = (await response.json()) as ApiResponse<T>
  if (!response.ok || !body.success) {
    throw new Error(body.message || '请求失败')
  }
  return body.data
}
