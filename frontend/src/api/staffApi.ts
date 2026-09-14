import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type { Order } from '@/types/order'
import type { Payment } from '@/types/wallet'


export type StaffDashboard = {
  asOf: string
  ordersToday: number
  revenueToday: number
  preparing: number
  ready: number
  pending: number
  openComplaints: number
  lowStock: number
}

export type StaffInventoryItem = {
  productId: string
  name: string
  quantity: number
  available: boolean
  lowStock: boolean
  soldOut: boolean
}

export type StaffOrderDetail = {
  order: Order
  payment: Payment | null
}

export type StaffSupportMessage = {
  id: string
  userId: string
  email: string
  fullName: string
  subject: string
  body: string
  status: string
  createdAt: string
}

export const staffApi = {
  dashboard() {
    return apiClient.get<StaffDashboard>('/api/staff/dashboard').then((response) => response.data)
  },

  inventory() {
    return apiClient.get<StaffInventoryItem[]>('/api/staff/inventory').then((response) => response.data)
  },

  setStock(productId: string, quantity: number) {
    return apiClient.put(`/api/inventory/${productId}`, { quantity }).then((response) => response.data)
  },

  orders(page = 0, size = 50) {
    return apiClient
      .get<PageResponse<Order>>('/api/staff/orders', { params: { page, size } })
      .then((response) => response.data)
  },

  order(orderId: string) {
    return apiClient.get<StaffOrderDetail>(`/api/staff/orders/${orderId}`).then((response) => response.data)
  },

  collectCod(orderId: string) {
    return apiClient.post<Payment>(`/api/staff/orders/${orderId}/collect-cod`).then((response) => response.data)
  },

  support(page = 0, size = 50) {
    return apiClient
      .get<PageResponse<StaffSupportMessage>>('/api/staff/support', { params: { page, size } })
      .then((response) => response.data)
  },

  closeSupport(messageId: string) {
    return apiClient
      .patch<StaffSupportMessage>(`/api/staff/support/${messageId}/close`)
      .then((response) => response.data)
  },
}
