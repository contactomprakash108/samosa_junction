
export type Wallet = {
  id: string
  balance: number
  currency: string
}

export type WalletTransactionType = 'CREDIT' | 'DEBIT' | 'REFUND'
export type WalletTransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED'

export type WalletTransaction = {
  id: string
  type: WalletTransactionType
  amount: number
  referenceId: string | null
  status: WalletTransactionStatus
  createdAt: string
}

export type PaymentMethod = 'WALLET' | 'CARD' | 'COD'
export type PaymentStatus = 'INITIATED' | 'SUCCESS' | 'FAILED' | 'REFUNDED'

export type Payment = {
  id: string
  orderId: string
  amount: number
  method: PaymentMethod
  status: PaymentStatus
  failureReason: string | null
  createdAt: string
}
