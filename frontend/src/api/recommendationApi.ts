import { apiClient } from '@/api/client'
import type { RecommendationResponse } from '@/types/product'


export const recommendationApi = {
  list(limit = 4) {
    return apiClient
      .get<RecommendationResponse>('/api/recommendations', { params: { limit } })
      .then((response) => response.data)
  },
}
