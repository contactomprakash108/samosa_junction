import { createContext, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/api/authApi'
import { tokenStore } from '@/api/tokenStore'
import { userApi } from '@/api/userApi'
import { useToken } from '@/hooks/useToken'
import type { LoginRequest, RegisterRequest } from '@/types/auth'
import type { User } from '@/types/user'


export type AuthStatus = 'loading' | 'anonymous' | 'authenticated'

export type AuthContextValue = {
  status: AuthStatus
  user: User | undefined
  login: (body: LoginRequest) => Promise<void>
  register: (body: RegisterRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const token = useToken()
  const queryClient = useQueryClient()

  const meQuery = useQuery({
    queryKey: ['users', 'me'],
    queryFn: userApi.me,
    enabled: Boolean(token),
    retry: false,
    staleTime: 60_000,
  })

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess(response) {
      tokenStore.set(response.accessToken)
      queryClient.setQueryData(['users', 'me'], response.user)
    },
  })

  const registerMutation = useMutation({
    mutationFn: authApi.register,
    onSuccess(response) {
      tokenStore.set(response.accessToken)
      queryClient.setQueryData(['users', 'me'], response.user)
    },
  })

  let status: AuthStatus = 'anonymous'
  if (token) {
    if ((meQuery.isLoading || meQuery.isFetching) && !meQuery.data) {
      status = 'loading'
    } else if (meQuery.data) {
      status = 'authenticated'
    }
  }

  const value: AuthContextValue = {
    status,
    user: meQuery.data,
    login: async (body) => {
      await loginMutation.mutateAsync(body)
    },
    register: async (body) => {
      await registerMutation.mutateAsync(body)
    },
    logout() {
      tokenStore.clear()
      queryClient.removeQueries({ queryKey: ['users', 'me'] })
        queryClient.removeQueries({ queryKey: ['recommendations'] })
        queryClient.removeQueries({ queryKey: ['cart'] })
        queryClient.removeQueries({ queryKey: ['wallet'] })
        queryClient.removeQueries({ queryKey: ['orders'] })
        queryClient.removeQueries({ queryKey: ['complaints'] })
        queryClient.removeQueries({ queryKey: ['staff'] })
        queryClient.removeQueries({ queryKey: ['kitchen'] })
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
