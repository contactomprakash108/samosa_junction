import axios, { type AxiosError } from 'axios'
import { tokenStore } from '@/api/tokenStore'
import type { ApiErrorBody } from '@/types/api'
import { ApiRequestError } from '@/utils/errors'


const AUTH_PATHS = new Set([
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
])

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    const requestUrl = error.config?.url ?? ''
    const isAuthCall = AUTH_PATHS.has(requestUrl)
    if (error.response?.status === 401 && !isAuthCall) {
      tokenStore.clear()
    }

    const body = error.response?.data
    if (body && typeof body.code === 'string' && typeof body.message === 'string') {
      return Promise.reject(new ApiRequestError(body))
    }

    if (!error.response) {
      return Promise.reject(new Error('Unable to reach Samosa Junction. Please try again in a moment.'))
    }

    return Promise.reject(error)
  },
)
