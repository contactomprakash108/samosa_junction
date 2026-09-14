import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { staffApi } from '@/api/staffApi'
import type { OrderItem } from '@/types/order'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'
import { formatInr } from '@/utils/money'


export function StaffOrderDetailPage() {
  const { orderId } = useParams()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['staff', 'orders', orderId],
    queryFn: () => staffApi.order(orderId!),
    enabled: Boolean(orderId),
    refetchInterval: 5000,
    retry: false,
  })
  const collect = useMutation({
    mutationFn: () => staffApi.collectCod(orderId!),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['staff', 'orders', orderId] })
      void queryClient.invalidateQueries({ queryKey: ['staff', 'orders'] })
    },
  })

  if (query.isLoading) {
    return <p className="text-zinc-400">Loading order…</p>
  }
  if (query.isError || !query.data) {
    return <p className="text-red-400">{errorMessage(query.error, 'Unable to load this order.')}</p>
  }

  const { order, payment } = query.data
  const cashDue = payment?.method === 'COD' && payment.status === 'INITIATED'
  const canCollect = cashDue && order.status === 'DELIVERED'

  return (
    <article className="space-y-4">
      <Link to="/staff/orders" className="text-sm text-amber-400">
        Orders
      </Link>
      <h1 className="font-mono text-2xl">#{order.id.slice(0, 8)}</h1>
      <p>
        {formatEnum(order.status)} · {formatDateTime(order.createdAt)}
      </p>
      <p className="text-sm text-zinc-400">
        {order.recipientName}
        <br />
        {order.addressLine1}, {order.city}
      </p>
      <ul className="space-y-1 text-sm">
        {order.items.map((item: OrderItem) => (
          <li key={`${item.productId}-${item.name}`}>
            {item.quantity} × {item.name} — {formatInr(item.lineTotal)}
          </li>
        ))}
      </ul>
      <p className="font-semibold">Total {formatInr(order.total)}</p>
      {payment ? (
        <p className="text-sm text-zinc-400">
          Payment {payment.status} · {payment.method === 'COD' ? 'Cash on delivery' : payment.method} ·{' '}
          {formatInr(payment.amount)}
        </p>
      ) : (
        <p className="text-sm text-zinc-400">No payment row yet.</p>
      )}
      {cashDue && order.status !== 'DELIVERED' ? (
        <p className="text-sm text-amber-400">Cash is due from the rider after delivery.</p>
      ) : null}
      {canCollect ? (
        <div className="space-y-2">
          {collect.isError ? <p className="text-red-400">{errorMessage(collect.error)}</p> : null}
          <button
            type="button"
            disabled={collect.isPending}
            onClick={() => collect.mutate()}
            className="rounded-md bg-amber-500 px-4 py-2 text-sm font-semibold text-zinc-950 disabled:opacity-50"
          >
            Mark cash collected
          </button>
        </div>
      ) : null}
    </article>
  )
}
