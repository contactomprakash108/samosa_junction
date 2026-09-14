import { useSyncExternalStore } from 'react'
import { tokenStore } from '@/api/tokenStore'


export function useToken() {
  return useSyncExternalStore(tokenStore.subscribe, tokenStore.get, () => null)
}
