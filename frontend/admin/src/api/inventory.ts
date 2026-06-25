import { request } from './http'

export type Warehouse = { id: number; code: string; name: string; warehouseType: string }
export type Item = { id: number; code: string; name: string; itemType: string; unit: string; latestPrice: number; materialTag?: string; primaryImageUrl?: string }
export type ItemImage = { id: number; itemId: number; attachmentId: number; usageType: string; sortOrder: number; url: string }
export type Stock = { id: number; warehouseId: number; itemId: number; quantity: number }
export type StockView = {
  id: number
  warehouseId: number
  itemId: number
  quantity: number
  ownerUserId: number
  ownerName: string
  itemName: string
  itemCode: string
  warehouseName: string
  warehouseType: string
  primaryImageUrl?: string
  stockImages: StockImage[]
}
export type StockSummary = {
  warehouseId: number
  warehouseName: string
  warehouseType: string
  itemId: number
  itemName: string
  itemCode: string
  quantity: number
}
export type StockImage = { id: number; stockId: number; attachmentId: number; usageType: string; sortOrder: number; url: string }
export type Price = { id: number; itemId: number; priceType: string; price: number; effectiveAt: string }
export type StockTxn = {
  id: number
  txnType: string
  txnTypeName: string
  bizNo: string
  warehouseId: number
  warehouseName: string
  itemId: number
  itemName: string
  itemCode: string
  quantityDelta: number
  operatorUserId: number
  operatorName: string
  occurredAt: string
}
export type InboundOrder = {
  id: number
  orderNo: string
  oaInstanceId: number
  warehouseId: number
  itemId: number
  quantity: number
  unitPrice: number
  amount: number
  status: string
  reimbursementLinked?: boolean
}

export const inventoryApi = {
  warehouses: () => request<Warehouse[]>('/api/v1/inventory/warehouses'),
  items: (keyword = '') => request<Item[]>(`/api/v1/inventory/items${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),
  createItem: (body: { code: string; name: string; itemType: string; unit: string; latestPrice: number; materialTag?: string }) =>
    request<Item>('/api/v1/inventory/items', { method: 'POST', body: JSON.stringify(body) }),
  updateItem: (id: number, body: { code?: string; name?: string; itemType?: string; unit?: string; latestPrice?: number; materialTag?: string }) =>
    request<Item>(`/api/v1/inventory/items/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  itemImages: (itemId: number) => request<ItemImage[]>(`/api/v1/inventory/items/${itemId}/images`),
  addItemImage: (itemId: number, body: { attachmentId: number; primary: boolean }) =>
    request<Item>(`/api/v1/inventory/items/${itemId}/images`, { method: 'POST', body: JSON.stringify(body) }),
  setMainItemImage: (itemId: number, imageId: number) =>
    request<Item>(`/api/v1/inventory/items/${itemId}/images/${imageId}/main`, { method: 'PUT' }),
  deleteItemImage: (itemId: number, imageId: number) =>
    request<void>(`/api/v1/inventory/items/${itemId}/images/${imageId}`, { method: 'DELETE' }),
  prices: () => request<Price[]>('/api/v1/inventory/prices'),
  stockSummary: (keyword = '') => request<StockSummary[]>(`/api/v1/inventory/stock-summary${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),
  stocks: (keyword = '') => request<StockView[]>(`/api/v1/inventory/stocks${keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''}`),
  transactions: () => request<StockTxn[]>('/api/v1/inventory/stock-transactions'),
  stockImages: (stockId: number) => request<StockImage[]>(`/api/v1/inventory/stocks/${stockId}/images`),
  addStockImage: (stockId: number, body: { attachmentId: number; usageType?: string }) =>
    request<StockImage>(`/api/v1/inventory/stocks/${stockId}/images`, { method: 'POST', body: JSON.stringify(body) }),
  deleteStockImage: (stockId: number, imageId: number) =>
    request<void>(`/api/v1/inventory/stocks/${stockId}/images/${imageId}`, { method: 'DELETE' }),
  createStock: (body: { warehouseId: number; itemId: number; quantity: number }) =>
    request<Stock>('/api/v1/inventory/stocks', { method: 'POST', body: JSON.stringify(body) }),
  adjustStock: (body: { warehouseId: number; itemId: number; quantity: number; reason?: string }) =>
    request<Stock>('/api/v1/inventory/stocks/adjust', { method: 'POST', body: JSON.stringify(body) }),
  inboundOrders: () => request<InboundOrder[]>('/api/v1/inventory/inbound-orders'),
  unlinkedInboundOrdersForReimbursement: () => request<InboundOrder[]>('/api/v1/inventory/inbound-orders/unlinked-for-reimbursement'),
  outboundOrders: () => request<Record<string, unknown>[]>('/api/v1/inventory/outbound-orders'),
}
