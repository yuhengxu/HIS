import { currentUserId, request } from './http'

export type AttachmentRecord = {
  id: number
  bizType?: string
  bizId?: number
  usageType: string
  originalName: string
  contentType: string
  sizeBytes: number
  url: string
}

export type UploadRequest = {
  bizType?: string
  bizId?: number
  usageType: 'material_image' | 'reimbursement_voucher' | 'oa_attachment'
  originalName: string
  contentType: string
  contentBase64: string
}

export async function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export const attachmentApi = {
  upload: (body: UploadRequest) => request<AttachmentRecord>('/api/v1/attachments', { method: 'POST', body: JSON.stringify(body) }),
  get: (id: number) => request<AttachmentRecord>(`/api/v1/attachments/${id}`),
  bind: (id: number, bizType: string, bizId: number) =>
    request<AttachmentRecord>(`/api/v1/attachments/${id}/bind`, { method: 'POST', body: JSON.stringify({ bizType, bizId }) }),
  delete: (id: number) => request<void>(`/api/v1/attachments/${id}`, { method: 'DELETE' }),
  uploadFile: async (file: File, usageType: UploadRequest['usageType'], bizType?: string, bizId?: number) => {
    const contentBase64 = await fileToBase64(file)
    return attachmentApi.upload({
      bizType,
      bizId,
      usageType,
      originalName: file.name,
      contentType: file.type || 'application/octet-stream',
      contentBase64,
    })
  },
}

export function attachmentContentUrl(id: number) {
  const userId = currentUserId()
  return `/api/v1/attachments/${id}/content${userId ? '' : ''}`
}
