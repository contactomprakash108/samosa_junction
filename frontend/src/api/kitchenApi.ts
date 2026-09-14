import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type { Order, OrderStatus } from '@/types/order'


export const kitchenApi = {
  list(page = 0, size = 100) {
    return apiClient
      .get<PageResponse<Order>>('/api/kitchen/orders', { params: { page, size } })
      .then((response) => response.data)
  },

  advance(orderId: string, status: OrderStatus) {
    return apiClient.patch<Order>(`/api/kitchen/orders/${orderId}`, { status }).then((response) => response.data)
  },
}
