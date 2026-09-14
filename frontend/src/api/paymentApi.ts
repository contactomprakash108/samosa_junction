import axios from 'axios'
import { apiClient } from '@/api/client'
import type { PageResponse } from '@/types/api'
import type { Payment } from '@/types/wallet'


export class PaymentFailedError extends Error {
  readonly payment: Payment

  constructor(payment: Payment) {
    super(payment.failureReason ?? 'Payment failed.')
    this.name = 'PaymentFailedError'
    this.payment = payment
  }
}

export function isPaymentFailedError(error: unknown): error is PaymentFailedError {
  return error instanceof PaymentFailedError
}

function asPayment(value: unknown): Payment | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const candidate = value as Payment
  if (typeof candidate.id === 'string' && typeof candidate.status === 'string') {
    return candidate
  }
  return null
}

export const paymentApi = {
  async pay(orderId: string, idempotencyKey: string, method: 'WALLET' | 'CARD' = 'WALLET') {
    try {
      const response = await apiClient.post<Payment>(
        '/api/payments',
        { orderId, method },
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const payment = asPayment(error.response?.data)
        if (error.response?.status === 409 && payment?.status === 'FAILED') {
          throw new PaymentFailedError(payment)
        }
      }
      throw error
    }
  },

  list(page = 0, size = 20) {
    return apiClient
      .get<PageResponse<Payment>>('/api/payments', { params: { page, size } })
      .then((response) => response.data)
  },

  refund(paymentId: string) {
    return apiClient.post<Payment>(`/api/payments/${paymentId}/refund`).then((response) => response.data)
  },
}
