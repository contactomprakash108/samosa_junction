import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type { DeliveryAddress, Order } from '@/types/order'


export const orderApi = {
  list(page = 0, size = 20) {
    return apiClient
      .get<PageResponse<Order>>('/api/orders', { params: { page, size } })
      .then((response) => response.data)
  },

  get(orderId: string) {
    return apiClient.get<Order>(`/api/orders/${orderId}`).then((response) => response.data)
  },

  create(delivery: DeliveryAddress, payNow: boolean, idempotencyKey: string, paymentMethod: 'WALLET' | 'CARD' | 'COD' = 'WALLET') {
    return apiClient
      .post<Order>('/api/orders', { delivery, payNow, paymentMethod }, { headers: { 'Idempotency-Key': idempotencyKey } })
      .then((response) => response.data)
  },

  cancel(orderId: string) {
    return apiClient.post<Order>(`/api/orders/${orderId}/cancel`).then((response) => response.data)
  },
}
