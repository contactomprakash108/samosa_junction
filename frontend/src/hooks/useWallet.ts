import { useQuery } from '@tanstack/react-query'
import { walletApi } from '@/api/walletApi'
import { useAuth } from '@/hooks/useAuth'


export const walletQueryKey = ['wallet'] as const

export function useWallet() {
  const { status } = useAuth()
  return useQuery({
    queryKey: walletQueryKey,
    queryFn: walletApi.get,
    enabled: status === 'authenticated',
    retry: false,
  })
}
