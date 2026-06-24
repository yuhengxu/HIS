import { request } from './http'

export type Warehouse = { id: number; code: string; name: string; warehouseType: string }
export type Item = { id: number; code: string; name: string; itemType: string; unit: string; latestPrice: number }
export type Stock = { id: number; warehouseId: number; itemId: number; quantity: number }
export type Price = { id: number; itemId: number; priceType: string; price: number; effectiveAt: string }

export const inventoryApi = {
  warehouses: () => request<Warehouse[]>('/api/v1/inventory/warehouses'),
  items: () => request<Item[]>('/api/v1/inventory/items'),
  createItem: (body: { code: string; name: string; itemType: string; unit: string; latestPrice: number }) =>
    request<Item>('/api/v1/inventory/items', { method: 'POST', body: JSON.stringify(body) }),
  prices: () => request<Price[]>('/api/v1/inventory/prices'),
  stocks: (keyword = '') => request<Stock[]>(`/api/v1/inventory/stocks${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),
  createStock: (body: { warehouseId: number; itemId: number; quantity: number }) =>
    request<Stock>('/api/v1/inventory/stocks', { method: 'POST', body: JSON.stringify(body) }),
  inboundOrders: () => request<Record<string, unknown>[]>('/api/v1/inventory/inbound-orders'),
  outboundOrders: () => request<Record<string, unknown>[]>('/api/v1/inventory/outbound-orders'),
}
