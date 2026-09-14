import type { OrderStatus } from '@/types/order'


export function formatEnum(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export function isPaidOrder(status: OrderStatus) {
  return status !== 'CREATED' && status !== 'CANCELLED'
}
