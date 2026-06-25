import { request } from './http'

export type MenuRecord = {
  id: number
  parentId?: number
  code: string
  name: string
  path: string
  icon?: string
  sortOrder: number
  status: string
}

export type MenuTreeNode = {
  menu: MenuRecord
  children: MenuTreeNode[]
}

export const systemApi = {
  menus: () => request<MenuRecord[]>('/api/v1/system/menus'),
  createMenu: (body: { code: string; name: string; parentId?: number; path: string; icon?: string; sortOrder?: number }) =>
    request<MenuRecord>('/api/v1/system/menus', { method: 'POST', body: JSON.stringify(body) }),
  updateMenu: (id: number, body: Partial<MenuRecord>) =>
    request<MenuRecord>(`/api/v1/system/menus/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteMenu: (id: number) => request<void>(`/api/v1/system/menus/${id}`, { method: 'DELETE' }),
  roleMenus: (roleCode: string) => request<number[]>(`/api/v1/system/menus/roles/${roleCode}`),
  saveRoleMenus: (roleCode: string, menuIds: number[]) =>
    request<void>(`/api/v1/system/menus/roles/${roleCode}`, { method: 'PUT', body: JSON.stringify(menuIds) }),
  myMenus: () => request<MenuTreeNode[]>('/api/v1/me/menus'),
}
