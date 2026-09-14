import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { orderApi } from '@/api/orderApi'
import { Alert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'


export function OrdersPage() {
  const query = useQuery({
    queryKey: ['orders'],
    queryFn: () => orderApi.list(),
    retry: false,
  })

  if (query.isLoading) {
    return <Spinner label="Loading orders…" />
  }
  if (query.isError) {
    return <Alert>{errorMessage(query.error, 'Unable to load orders.')}</Alert>
  }
  if (!query.data?.content.length) {
    return (
      <section className="space-y-4">
        <h1 className="font-display text-3xl">Orders</h1>
        <Alert tone="info">No orders yet. The menu is a good place to start.</Alert>
      </section>
    )
  }

  return (
    <section className="space-y-4">
      <h1 className="font-display text-3xl">Orders</h1>
      <ul className="divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
        {query.data.content.map((order) => (
          <li key={order.id}>
            <Link to={`/orders/${order.id}`} className="flex flex-wrap items-center justify-between gap-3 px-4 py-4">
              <div>
                <p className="font-medium">{formatEnum(order.status)}</p>
                <p className="text-sm text-ink/55">
                  {formatDateTime(order.createdAt)} · {order.items.length} item(s)
                </p>
              </div>
              <p className="font-semibold">{formatInr(order.total)}</p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
