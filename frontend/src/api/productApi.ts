import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type { Category, Product, ProductListQuery, ProductWrite } from '@/types/product'


export const productApi = {
  categories() {
    return apiClient.get<Category[]>('/api/categories').then((response) => response.data)
  },

  list(query: ProductListQuery = {}) {
    return apiClient
      .get<PageResponse<Product>>('/api/products', { params: query })
      .then((response) => response.data)
  },

  getById(id: string) {
    return apiClient.get<Product>(`/api/products/${id}`).then((response) => response.data)
  },

  create(body: ProductWrite) {
    return apiClient.post<Product>('/api/products', body).then((response) => response.data)
  },

  update(id: string, body: ProductWrite) {
    return apiClient.put<Product>(`/api/products/${id}`, body).then((response) => response.data)
  },

  remove(id: string) {
    return apiClient.delete(`/api/products/${id}`)
  },

  uploadImage(id: string, file: File) {
    const form = new FormData()
    form.append('file', file)
    return apiClient
      .post<Product>(`/api/products/${id}/image`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
      .then((response) => response.data)
  },
}
