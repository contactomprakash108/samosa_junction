import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { staffApi } from '@/api/staffApi'
import { errorMessage } from '@/utils/errors'
import { formatDateTime } from '@/utils/status'


export function StaffSupportPage() {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['staff', 'support'],
    queryFn: () => staffApi.support(),
    refetchInterval: 8000,
    retry: false,
  })
  const close = useMutation({
    mutationFn: (id: string) => staffApi.closeSupport(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['staff', 'support'] }),
  })

  const rows = query.data?.content ?? []

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Support</h1>
      <p className="text-sm text-zinc-400">Open threads need a close when you’ve handled them. Customers see Open or Closed on Help → Talk to support.</p>
      {query.isLoading ? <p className="text-zinc-400">Loading messages…</p> : null}
      {query.isError ? <p className="text-red-400">{errorMessage(query.error)}</p> : null}
      {close.isError ? <p className="text-red-400">{errorMessage(close.error)}</p> : null}
      {!rows.length ? <p className="text-zinc-400">No support messages.</p> : null}
      <ul className="space-y-3">
        {rows.map((row) => (
          <li key={row.id} className="rounded-lg border border-zinc-800 bg-zinc-900 p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium">{row.subject}</p>
                <p className="text-xs text-zinc-500">
                  {row.status} · {row.fullName} · {row.email} · {formatDateTime(row.createdAt)}
                </p>
              </div>
              {row.status !== 'CLOSED' ? (
                <button
                  type="button"
                  disabled={close.isPending}
                  className="rounded-md bg-amber-500 px-3 py-1.5 text-sm font-semibold text-zinc-950 disabled:opacity-50"
                  onClick={() => close.mutate(row.id)}
                >
                  Close
                </button>
              ) : (
                <span className="text-xs text-zinc-500">Closed</span>
              )}
            </div>
            <p className="mt-2 text-sm text-zinc-300">{row.body}</p>
          </li>
        ))}
      </ul>
    </section>
  )
}
