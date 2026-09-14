import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cartApi } from '@/api/cartApi'
import { useAuth } from '@/hooks/useAuth'


export const cartQueryKey = ['cart'] as const

export function useCart() {
  const { status } = useAuth()
  const queryClient = useQueryClient()
  const enabled = status === 'authenticated'

  const query = useQuery({
    queryKey: cartQueryKey,
    queryFn: cartApi.get,
    enabled,
    retry: false,
  })

  const emptyCart = { items: [], totalQuantity: 0, subtotal: 0 }

  const invalidate = () => queryClient.invalidateQueries({ queryKey: cartQueryKey })

  const resetAfterCheckout = async () => {
    queryClient.setQueryData(cartQueryKey, emptyCart)
    try {
      await cartApi.clear()
    } catch {
      // Backend already clears after commit; this is a second pass if that lagged.
    }
    await queryClient.invalidateQueries({ queryKey: cartQueryKey })
  }

  const add = useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      cartApi.add(productId, quantity),
    onSuccess: (cart) => queryClient.setQueryData(cartQueryKey, cart),
  })

  const update = useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      cartApi.update(productId, quantity),
    onSuccess: (cart) => queryClient.setQueryData(cartQueryKey, cart),
  })

  const remove = useMutation({
    mutationFn: (productId: string) => cartApi.remove(productId),
    onSuccess: (cart) => queryClient.setQueryData(cartQueryKey, cart),
  })

  const clear = useMutation({
    mutationFn: cartApi.clear,
    onSuccess: invalidate,
  })

  return { query, add, update, remove, clear, invalidate, resetAfterCheckout }
}
