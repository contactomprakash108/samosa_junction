
export type User = {
  id: string
  email: string
  fullName: string
  phone: string | null
  recipientName: string | null
  addressLine1: string | null
  city: string | null
  state: string | null
  pincode: string | null
  hasDefaultAddress: boolean
  roles: string[]
  createdAt: string
}

export type UpdateProfileRequest = {
  fullName: string
  phone: string
  recipientName: string
  addressLine1: string
  city: string
  state: string
  pincode: string
}
