import { apiClient } from '@/api/client'
import type { AssistantHistoryMessage } from '@/types/assistant'


export type AssistantProduct = {
  id: string
  name: string
  price: number
  stock: number
}

export type AssistantChat = {
  reply: string
  products: AssistantProduct[]
}

export const assistantApi = {
  chat(message: string, history: AssistantHistoryMessage[] = []) {
    return apiClient
      .post<AssistantChat>('/api/assistant/chat', { message, history })
      .then((response) => response.data)
  },
}
