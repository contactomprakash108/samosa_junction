import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { complaintApi } from '@/api/complaintApi'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'


export function StaffComplaintsPage() {
  const query = useQuery({
    queryKey: ['staff', 'complaints'],
    queryFn: () => complaintApi.list(0, 50),
    refetchInterval: 8000,
    retry: false,
  })

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Complaints</h1>
      {query.isLoading ? <p className="text-zinc-400">Loading complaints…</p> : null}
      {query.isError ? <p className="text-red-400">{errorMessage(query.error)}</p> : null}
      {!query.data?.content.length ? <p className="text-zinc-400">No complaints.</p> : null}
      <ul className="divide-y divide-zinc-800 rounded-lg border border-zinc-800">
        {query.data?.content.map((complaint) => (
          <li key={complaint.id}>
            <Link to={`/staff/complaints/${complaint.id}`} className="block px-4 py-3 hover:bg-zinc-900">
              <p className="font-medium">
                {formatEnum(complaint.category)} · {formatEnum(complaint.status)}
              </p>
              <p className="text-xs text-zinc-500">{formatDateTime(complaint.createdAt)}</p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
