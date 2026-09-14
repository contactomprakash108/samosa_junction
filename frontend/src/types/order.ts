
export type OrderStatus =
  | 'CREATED'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'

export type OrderItem = {
  productId: string
  name: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export type Order = {
  id: string
  status: OrderStatus
  recipientName: string
  addressLine1: string
  city: string
  state: string
  pincode: string
  total: number
  items: OrderItem[]
  createdAt: string
}

export type DeliveryAddress = {
  recipientName: string
  line1: string
  city: string
  state: string
  pincode: string
}

export const CANCELLABLE_STATUSES: OrderStatus[] = ['CREATED', 'CONFIRMED', 'PREPARING']

export const LIFECYCLE: OrderStatus[] = [
  'CREATED',
  'CONFIRMED',
  'PREPARING',
  'READY',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
]
