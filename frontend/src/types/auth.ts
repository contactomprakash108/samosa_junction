import type { User } from '@/types/user'


export type AuthResponse = {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export type LoginRequest = {
  email: string
  password: string
}

export type RegisterRequest = {
  email: string
  password: string
  fullName: string
}

export type ForgotPasswordRequest = {
  email: string
}

export type ForgotPasswordResponse = {
  message: string
  resetPath?: string
}

export type ResetPasswordRequest = {
  token: string
  password: string
}

export type MessageResponse = {
  message: string
}
