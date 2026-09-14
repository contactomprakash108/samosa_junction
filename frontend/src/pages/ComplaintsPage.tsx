import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { complaintApi } from '@/api/complaintApi'
import { Alert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'


export function ComplaintsPage() {
  const query = useQuery({
    queryKey: ['complaints'],
    queryFn: () => complaintApi.list(),
    retry: false,
  })

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link to="/help" className="text-sm font-semibold text-saffron">
            Help
          </Link>
          <h1 className="font-display text-3xl">Complaints</h1>
          <p className="mt-1 text-sm text-ink/60">
            Tickets on paid orders. If you raise a complaint, an executive will call you soon on your account email.
          </p>
        </div>
        <Link to="/complaints/new" className="rounded-full bg-saffron px-4 py-2 text-sm font-semibold text-cream">
          New complaint
        </Link>
      </div>
      {query.isLoading ? <Spinner label="Loading complaints…" /> : null}
      {query.isError ? <Alert>{errorMessage(query.error)}</Alert> : null}
      {query.data?.content.length === 0 ? <Alert tone="info">No complaints yet.</Alert> : null}
      {query.data?.content.length ? (
        <ul className="divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
          {query.data.content.map((complaint) => (
            <li key={complaint.id}>
              <Link to={`/complaints/${complaint.id}`} className="block px-4 py-4">
                <p className="font-medium">
                  {formatEnum(complaint.category)} · {formatEnum(complaint.status)}
                </p>
                <p className="text-sm text-ink/55">{formatDateTime(complaint.createdAt)}</p>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  )
}
