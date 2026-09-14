import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { staffApi } from '@/api/staffApi'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'
import { formatInr } from '@/utils/money'


export function StaffOrdersPage() {
  const query = useQuery({
    queryKey: ['staff', 'orders'],
    queryFn: () => staffApi.orders(),
    refetchInterval: 8000,
    retry: false,
  })

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Orders</h1>
      {query.isLoading ? <p className="text-zinc-400">Loading orders…</p> : null}
      {query.isError ? <p className="text-red-400">{errorMessage(query.error)}</p> : null}
      {!query.data?.content.length ? <p className="text-zinc-400">No orders yet.</p> : null}
      <ul className="divide-y divide-zinc-800 rounded-lg border border-zinc-800">
        {query.data?.content.map((order) => (
          <li key={order.id}>
            <Link to={`/staff/orders/${order.id}`} className="flex items-center justify-between gap-4 px-4 py-3 hover:bg-zinc-900">
              <div>
                <p className="font-mono text-sm">#{order.id.slice(0, 8)}</p>
                <p className="text-xs text-zinc-500">{formatDateTime(order.createdAt)}</p>
              </div>
              <p className="text-sm">{formatEnum(order.status)}</p>
              <p className="tabular-nums">{formatInr(order.total)}</p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
