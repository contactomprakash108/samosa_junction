
export type CartItem = {
  productId: string
  name: string
  quantity: number
  unitPrice: number
  lineTotal: number
  available: boolean
  productMissing: boolean
  stock: number
}

export type Cart = {
  items: CartItem[]
  totalQuantity: number
  subtotal: number
}
