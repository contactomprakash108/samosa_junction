import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'


export type SupportMessage = {
  id: string
  subject: string
  body: string
  status: string
  createdAt: string
}

export const supportApi = {
  create(body: { subject: string; body: string }) {
    return apiClient.post<SupportMessage>('/api/support', body).then((response) => response.data)
  },

  list(page = 0, size = 20) {
    return apiClient
      .get<PageResponse<SupportMessage>>('/api/support', { params: { page, size } })
      .then((response) => response.data)
  },
}
