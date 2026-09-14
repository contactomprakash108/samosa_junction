import { apiClient } from '@/api/client'
import type {
  AuthResponse,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  LoginRequest,
  MessageResponse,
  RegisterRequest,
  ResetPasswordRequest,
} from '@/types/auth'


export const authApi = {
  register(body: RegisterRequest) {
    return apiClient.post<AuthResponse>('/api/auth/register', body).then((response) => response.data)
  },

  login(body: LoginRequest) {
    return apiClient.post<AuthResponse>('/api/auth/login', body).then((response) => response.data)
  },

  forgotPassword(body: ForgotPasswordRequest) {
    return apiClient.post<ForgotPasswordResponse>('/api/auth/forgot-password', body).then((response) => response.data)
  },

  resetPassword(body: ResetPasswordRequest) {
    return apiClient.post<MessageResponse>('/api/auth/reset-password', body).then((response) => response.data)
  },
}
