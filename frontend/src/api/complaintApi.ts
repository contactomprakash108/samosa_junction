import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type {
  Complaint,
  ComplaintCategory,
  ComplaintImage,
  ComplaintPriority,
  ComplaintStatus,
} from '@/types/complaint'


export const complaintApi = {
  list(page = 0, size = 20) {
    return apiClient
      .get<PageResponse<Complaint>>('/api/complaints', { params: { page, size, sort: 'createdAt,desc' } })
      .then((response) => response.data)
  },

  get(complaintId: string) {
    return apiClient.get<Complaint>(`/api/complaints/${complaintId}`).then((response) => response.data)
  },

  create(
    body: { orderId: string; category: ComplaintCategory; description: string; priority?: ComplaintPriority },
    idempotencyKey: string,
  ) {
    return apiClient
      .post<Complaint>('/api/complaints', body, { headers: { 'Idempotency-Key': idempotencyKey } })
      .then((response) => response.data)
  },

  update(complaintId: string, body: { status?: ComplaintStatus; priority?: ComplaintPriority }) {
    return apiClient.patch<Complaint>(`/api/complaints/${complaintId}`, body).then((response) => response.data)
  },

  images(complaintId: string) {
    return apiClient
      .get<ComplaintImage[]>(`/api/complaints/${complaintId}/images`)
      .then((response) => response.data)
  },

  uploadImage(complaintId: string, file: File) {
    const form = new FormData()
    form.append('file', file)
    return apiClient
      .post<ComplaintImage>(`/api/complaints/${complaintId}/images`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((response) => response.data)
  },

  deleteImage(complaintId: string, imageId: string) {
    return apiClient.delete(`/api/complaints/${complaintId}/images/${imageId}`)
  },
}
