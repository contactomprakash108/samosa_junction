import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { staffApi } from '@/api/staffApi'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'


export function StaffDashboardPage() {
  const query = useQuery({
    queryKey: ['staff', 'dashboard'],
    queryFn: staffApi.dashboard,
    refetchInterval: 8000,
    retry: false,
  })

  if (query.isLoading) {
    return <p className="text-zinc-400">Loading today’s board…</p>
  }
  if (query.isError) {
    return <p className="text-red-400">{errorMessage(query.error, 'Unable to load the dashboard.')}</p>
  }
  const data = query.data
  if (!data) {
    return <p className="text-zinc-400">No dashboard data yet.</p>
  }

  const tiles = [
    { label: 'Orders today', value: String(data.ordersToday), to: '/staff/orders' },
    { label: 'Revenue today', value: formatInr(data.revenueToday), to: '/staff/orders' },
    { label: 'Preparing', value: String(data.preparing), to: '/staff/kitchen' },
    { label: 'Ready', value: String(data.ready), to: '/staff/kitchen' },
    { label: 'Pending pay', value: String(data.pending), to: '/staff/orders' },
    { label: 'Open complaints', value: String(data.openComplaints), to: '/staff/complaints' },
    { label: 'Low stock', value: String(data.lowStock), to: '/staff/inventory' },
  ]

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Today</h1>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {tiles.map((tile) => (
          <Link
            key={tile.label}
            to={tile.to}
            className="rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-5 hover:border-amber-500/40"
          >
            <p className="text-xs tracking-wide text-zinc-500 uppercase">{tile.label}</p>
            <p className="mt-2 text-3xl font-semibold tabular-nums">{tile.value}</p>
          </Link>
        ))}
      </div>
    </section>
  )
}
