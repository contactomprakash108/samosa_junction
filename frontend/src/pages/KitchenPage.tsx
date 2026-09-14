import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { kitchenApi } from '@/api/kitchenApi'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'
import type { Order, OrderStatus } from '@/types/order'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'


const NEXT: Partial<Record<OrderStatus, OrderStatus>> = {
  CONFIRMED: 'PREPARING',
  PREPARING: 'READY',
  READY: 'OUT_FOR_DELIVERY',
  OUT_FOR_DELIVERY: 'DELIVERED',
}

export function KitchenPage() {
  const { user } = useAuth()
  const staff = Boolean(user?.roles.some((role) => role === 'STAFF' || role === 'ADMIN'))
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['kitchen', 'orders'],
    queryFn: () => kitchenApi.list(),
    enabled: staff,
    retry: false,
  })
  const advance = useMutation({
    mutationFn: ({ orderId, status }: { orderId: string; status: OrderStatus }) => kitchenApi.advance(orderId, status),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['kitchen', 'orders'] }),
  })

  if (!staff) {
    return <Alert>Kitchen is for staff only.</Alert>
  }
  if (query.isLoading) {
    return <Spinner label="Loading kitchen tickets…" />
  }
  if (query.isError) {
    return <Alert>{errorMessage(query.error, 'Unable to load the kitchen board.')}</Alert>
  }

  return (
    <section className="space-y-6">
      <div>
        <h1 className="font-display text-4xl">Kitchen</h1>
        <p className="mt-2 text-ink/65">Advance a ticket one step at a time. Customers can still cancel until it is ready.</p>
      </div>
      {advance.isError ? <Alert>{errorMessage(advance.error)}</Alert> : null}
      {!query.data?.content.length ? <Alert tone="info">No kitchen tickets yet.</Alert> : null}
      <ul className="space-y-3">
        {query.data?.content.map((order) => (
          <KitchenRow
            key={order.id}
            order={order}
            pending={advance.isPending}
            onAdvance={() => {
              const next = NEXT[order.status]
              if (next) {
                advance.mutate({ orderId: order.id, status: next })
              }
            }}
          />
        ))}
      </ul>
    </section>
  )
}

function KitchenRow({
  order,
  pending,
  onAdvance,
}: {
  order: Order
  pending: boolean
  onAdvance: () => void
}) {
  const next = NEXT[order.status]
  return (
    <li className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-ink/10 bg-white px-4 py-4">
      <div>
        <p className="font-semibold">{formatEnum(order.status)}</p>
        <p className="text-sm text-ink/60">
          {order.items.map((item) => `${item.quantity}× ${item.name}`).join(', ')}
        </p>
        <p className="text-xs text-ink/45">{formatDateTime(order.createdAt)}</p>
      </div>
      {next ? (
        <Button type="button" pending={pending} onClick={onAdvance}>
          Mark {formatEnum(next)}
        </Button>
      ) : (
        <span className="text-sm text-ink/50">Done</span>
      )}
    </li>
  )
}
