import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type { Wallet, WalletTransaction } from '@/types/wallet'


export const walletApi = {
  get() {
    return apiClient.get<Wallet>('/api/wallet').then((response) => response.data)
  },

  addMoney(amount: number, idempotencyKey: string, referenceId?: string) {
    return apiClient
      .post<Wallet>(
        '/api/wallet/add-money',
        { amount, referenceId },
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      .then((response) => response.data)
  },

  transactions(page = 0, size = 20) {
    return apiClient
      .get<PageResponse<WalletTransaction>>('/api/wallet/transactions', {
        params: { page, size, sort: 'createdAt,desc' },
      })
      .then((response) => response.data)
  },
}
