import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { kitchenApi } from '@/api/kitchenApi'
import type { Order, OrderStatus } from '@/types/order'
import { errorMessage, isApiRequestError } from '@/utils/errors'
import { formatDateTime } from '@/utils/status'
import { formatInr } from '@/utils/money'


const COLUMNS: { key: OrderStatus; title: string }[] = [
  { key: 'CONFIRMED', title: 'New' },
  { key: 'PREPARING', title: 'Preparing' },
  { key: 'READY', title: 'Ready' },
  { key: 'OUT_FOR_DELIVERY', title: 'Out' },
]

const NEXT: Partial<Record<OrderStatus, { status: OrderStatus; label: string }>> = {
  CONFIRMED: { status: 'PREPARING', label: 'Start preparing' },
  PREPARING: { status: 'READY', label: 'Mark ready' },
  READY: { status: 'OUT_FOR_DELIVERY', label: 'Out for delivery' },
  OUT_FOR_DELIVERY: { status: 'DELIVERED', label: 'Mark delivered' },
}

function staleKitchen(error: unknown) {
  if (!isApiRequestError(error)) {
    return false
  }
  return error.code === 'CONCURRENT_UPDATE' || error.message.includes('Kitchen can only')
}

export function StaffKitchenPage() {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['kitchen', 'orders'],
    queryFn: () => kitchenApi.list(0, 100),
    refetchInterval: 5000,
    retry: false,
  })
  const advance = useMutation({
    mutationFn: ({ orderId, status }: { orderId: string; status: OrderStatus }) => kitchenApi.advance(orderId, status),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['kitchen'] }),
    onError: (error) => {
      if (staleKitchen(error)) {
        void queryClient.invalidateQueries({ queryKey: ['kitchen'] })
      }
    },
  })

  const orders = query.data?.content ?? []
  const byStatus = (status: OrderStatus) => orders.filter((order) => order.status === status)

  return (
    <section className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">Kitchen queue</h1>
      </div>
      {query.isError ? <p className="text-red-400">{errorMessage(query.error)}</p> : null}
      {advance.isError ? (
        <p className="text-amber-400">
          {staleKitchen(advance.error)
            ? 'This order was already updated. Refreshing the latest status.'
            : errorMessage(advance.error)}
        </p>
      ) : null}
      {query.isLoading ? <p className="text-zinc-400">Loading tickets…</p> : null}
      <div className="grid gap-4 lg:grid-cols-4">
        {COLUMNS.map((column) => (
          <div key={column.key} className="rounded-lg border border-zinc-800 bg-zinc-900/60 p-3">
            <h2 className="mb-3 text-xs font-semibold tracking-wide text-zinc-400 uppercase">
              {column.title} · {byStatus(column.key).length}
            </h2>
            <ul className="space-y-3">
              {byStatus(column.key).map((order) => (
                <KitchenCard
                  key={order.id}
                  order={order}
                  pending={advance.isPending}
                  onAdvance={() => {
                    const next = NEXT[order.status]
                    if (next) {
                      advance.mutate({ orderId: order.id, status: next.status })
                    }
                  }}
                />
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  )
}

function KitchenCard({
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
    <li className="rounded-md border border-zinc-700 bg-zinc-950 p-4">
      <p className="font-mono text-xs text-zinc-500">#{order.id.slice(0, 8)}</p>
      <ul className="mt-2 space-y-1 text-sm">
        {order.items.map((item) => (
          <li key={`${item.productId}-${item.name}`}>
            {item.quantity} × {item.name}
          </li>
        ))}
      </ul>
      <p className="mt-2 text-sm text-zinc-300">Total {formatInr(order.total)}</p>
      <p className="text-xs text-zinc-500">Placed {formatDateTime(order.createdAt)}</p>
      {next ? (
        <button
          type="button"
          disabled={pending}
          onClick={onAdvance}
          className="mt-3 w-full rounded-md bg-amber-500 px-3 py-2 text-sm font-semibold text-zinc-950 disabled:opacity-50"
        >
          {next.label}
        </button>
      ) : null}
    </li>
  )
}
