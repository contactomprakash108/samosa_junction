import { apiClient } from '@/api/client'
import type { Cart } from '@/types/cart'


export const cartApi = {
  get() {
    return apiClient.get<Cart>('/api/cart').then((response) => response.data)
  },

  add(productId: string, quantity: number) {
    return apiClient
      .post<Cart>('/api/cart/items', { productId, quantity })
      .then((response) => response.data)
  },

  update(productId: string, quantity: number) {
    return apiClient
      .put<Cart>(`/api/cart/items/${productId}`, { quantity })
      .then((response) => response.data)
  },

  remove(productId: string) {
    return apiClient.delete<Cart>(`/api/cart/items/${productId}`).then((response) => response.data)
  },

  clear() {
    return apiClient.delete('/api/cart')
  },
}
