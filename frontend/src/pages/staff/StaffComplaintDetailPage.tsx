import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { complaintApi } from '@/api/complaintApi'
import { paymentApi } from '@/api/paymentApi'
import { staffApi } from '@/api/staffApi'
import type { ComplaintStatus } from '@/types/complaint'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'


export function StaffComplaintDetailPage() {
  const { complaintId } = useParams()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['complaints', complaintId],
    queryFn: () => complaintApi.get(complaintId!),
    enabled: Boolean(complaintId),
    retry: false,
  })
  const imagesQuery = useQuery({
    queryKey: ['complaints', complaintId, 'images'],
    queryFn: () => complaintApi.images(complaintId!),
    enabled: Boolean(complaintId),
    retry: false,
  })
  const orderQuery = useQuery({
    queryKey: ['staff', 'orders', query.data?.orderId],
    queryFn: () => staffApi.order(query.data!.orderId),
    enabled: Boolean(query.data?.orderId),
    retry: false,
  })

  const patch = useMutation({
    mutationFn: (status: ComplaintStatus) => complaintApi.update(complaintId!, { status }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['complaints', complaintId] })
      void queryClient.invalidateQueries({ queryKey: ['staff', 'complaints'] })
    },
  })

  const refund = useMutation({
    mutationFn: async () => {
      const paymentId = orderQuery.data?.payment?.id
      if (!paymentId) {
        throw new Error('No successful payment on this order.')
      }
      await paymentApi.refund(paymentId)
      if (query.data?.status === 'OPEN' || query.data?.status === 'IN_PROGRESS') {
        await complaintApi.update(complaintId!, { status: 'RESOLVED' })
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['complaints', complaintId] })
      void queryClient.invalidateQueries({ queryKey: ['staff'] })
    },
  })

  if (query.isLoading) {
    return <p className="text-zinc-400">Loading complaint…</p>
  }
  if (query.isError || !query.data) {
    return <p className="text-red-400">{errorMessage(query.error)}</p>
  }

  const complaint = query.data
  const images = imagesQuery.data ?? complaint.images ?? []

  return (
    <article className="space-y-4">
      <Link to="/staff/complaints" className="text-sm text-amber-400">
        Complaints
      </Link>
      <h1 className="text-2xl font-semibold">
        {formatEnum(complaint.category)} · {formatEnum(complaint.status)}
      </h1>
      <p className="text-sm text-zinc-400">{formatDateTime(complaint.createdAt)}</p>
      <p>
        Order{' '}
        <Link to={`/staff/orders/${complaint.orderId}`} className="font-mono text-amber-400">
          #{complaint.orderId.slice(0, 8)}
        </Link>
      </p>
      <p className="rounded-md border border-zinc-800 bg-zinc-900 p-4">{complaint.description}</p>
      {images.length ? (
        <ul className="grid gap-3 sm:grid-cols-3">
          {images.map((image) => (
            <li key={image.id}>
              {image.url ? <img src={image.url} alt={image.fileName} className="rounded-md border border-zinc-800" /> : null}
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-zinc-500">No images.</p>
      )}
      {patch.isError ? <p className="text-red-400">{errorMessage(patch.error)}</p> : null}
      {refund.isError ? <p className="text-red-400">{errorMessage(refund.error)}</p> : null}
      {orderQuery.data?.payment?.status === 'INITIATED' ? (
        <p className="text-sm text-amber-400">Collect cash on the order first, then you can refund.</p>
      ) : null}
      <div className="flex flex-wrap gap-2">
        {complaint.status === 'OPEN' ? (
          <Action label="Investigate" onClick={() => patch.mutate('IN_PROGRESS')} pending={patch.isPending} />
        ) : null}
        {complaint.status === 'OPEN' || complaint.status === 'IN_PROGRESS' ? (
          <>
            <Action
              label="Approve refund"
              onClick={() => refund.mutate()}
              pending={refund.isPending || orderQuery.isLoading}
            />
            <Action label="Resolve" onClick={() => patch.mutate('RESOLVED')} pending={patch.isPending} />
          </>
        ) : null}
      </div>
      {orderQuery.data?.payment ? (
        <p className="text-sm text-zinc-400">Payment {orderQuery.data.payment.status}</p>
      ) : null}
    </article>
  )
}

function Action({ label, onClick, pending }: { label: string; onClick: () => void; pending: boolean }) {
  return (
    <button
      type="button"
      disabled={pending}
      onClick={onClick}
      className="rounded-md bg-amber-500 px-4 py-2 text-sm font-semibold text-zinc-950 disabled:opacity-50"
    >
      {label}
    </button>
  )
}
