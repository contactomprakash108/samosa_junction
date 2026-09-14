import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { orderApi } from '@/api/orderApi'
import { isPaymentFailedError, paymentApi } from '@/api/paymentApi'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { useWallet } from '@/hooks/useWallet'
import { CANCELLABLE_STATUSES, LIFECYCLE } from '@/types/order'
import { newIdempotencyKey } from '@/utils/idempotency'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum } from '@/utils/status'
import { walletQueryKey } from '@/hooks/useWallet'


export function OrderDetailPage() {
  const { orderId } = useParams()
  const location = useLocation()
  const justPlaced = Boolean((location.state as { justPlaced?: boolean } | null)?.justPlaced)
  const queryClient = useQueryClient()
  const walletQuery = useWallet()
  const [payError, setPayError] = useState<string | null>(null)
  const [payMethod, setPayMethod] = useState<'WALLET' | 'CARD'>('WALLET')

  const query = useQuery({
    queryKey: ['orders', orderId],
    queryFn: () => orderApi.get(orderId!),
    enabled: Boolean(orderId),
    retry: false,
    refetchInterval: 8000,
  })
  const paymentsQuery = useQuery({
    queryKey: ['payments'],
    queryFn: () => paymentApi.list(0, 50),
    retry: false,
    refetchInterval: 8000,
  })

  const cancel = useMutation({
    mutationFn: () => orderApi.cancel(orderId!),
    onSuccess: (order) => {
      queryClient.setQueryData(['orders', orderId], order)
      void queryClient.invalidateQueries({ queryKey: ['orders'] })
      void queryClient.invalidateQueries({ queryKey: walletQueryKey })
    },
  })

  const pay = useMutation({
    mutationFn: () => paymentApi.pay(orderId!, newIdempotencyKey(), payMethod),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['orders', orderId] })
      void queryClient.invalidateQueries({ queryKey: ['orders'] })
      void queryClient.invalidateQueries({ queryKey: walletQueryKey })
      void queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })

  if (query.isLoading) {
    return <Spinner label="Loading order…" />
  }
  if (query.isError || !query.data) {
    return <Alert>{errorMessage(query.error, 'Unable to load this order.')}</Alert>
  }

  const order = query.data
  const payment = paymentsQuery.data?.content.find((item) => item.orderId === order.id)
  const cashOnDelivery = payment?.method === 'COD'

  return (
    <article className="space-y-6">
      {justPlaced ? <Alert tone="success">Order placed successfully.</Alert> : null}
      <div>
        <p className="text-xs tracking-wide text-ink/50 uppercase">Order</p>
        <h1 className="font-display text-3xl">{formatEnum(order.status)}</h1>
        <p className="text-sm text-ink/55">{formatDateTime(order.createdAt)}</p>
      </div>

      <ol className="grid grid-cols-2 gap-2 text-xs sm:grid-cols-6">
        {LIFECYCLE.map((status) => {
          const reached = lifecycleIndex(order.status) >= lifecycleIndex(status)
          return (
            <li
              key={status}
              className={`rounded-lg px-2 py-2 text-center ${
                order.status === 'CANCELLED'
                  ? 'bg-ink/5 text-ink/40'
                  : reached
                    ? 'bg-saffron text-cream'
                    : 'bg-ink/5 text-ink/40'
              }`}
            >
              {formatEnum(status)}
            </li>
          )
        })}
      </ol>
      {order.status === 'CANCELLED' ? <Alert tone="info">This order is cancelled.</Alert> : null}

      <section className="rounded-2xl border border-ink/10 bg-white p-4">
        <h2 className="font-semibold">Delivery</h2>
        <p className="mt-1 text-sm text-ink/70">
          {order.recipientName}
          <br />
          {order.addressLine1}, {order.city}, {order.state} {order.pincode}
        </p>
      </section>

      <ul className="divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
        {order.items.map((item) => (
          <li key={`${item.productId}-${item.name}`} className="flex justify-between px-4 py-3 text-sm">
            <span>
              {item.quantity} × {item.name}
            </span>
            <span>{formatInr(item.lineTotal)}</span>
          </li>
        ))}
      </ul>
      <p className="text-lg font-semibold">Total {formatInr(order.total)}</p>
      {cashOnDelivery ? (
        <Alert tone="info">
          {payment?.status === 'SUCCESS'
            ? 'Cash collected. This order is paid.'
            : 'Cash on delivery. Pay the rider. Kitchen will cook this before payment is marked collected.'}
        </Alert>
      ) : null}

      {order.status === 'CREATED' && !cashOnDelivery ? (
        <div className="space-y-3 rounded-2xl border border-ink/10 bg-white p-4">
          <p className="text-sm text-ink/70">
            Unpaid. Wallet {walletQuery.data ? formatInr(walletQuery.data.balance) : '…'}.
          </p>
          {payError ? <Alert>{payError}</Alert> : null}
          {pay.isError && !payError ? <Alert>{errorMessage(pay.error)}</Alert> : null}
          <fieldset className="space-y-2 text-sm">
            <legend className="font-medium">Pay with</legend>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                name="pay-method"
                checked={payMethod === 'WALLET'}
                onChange={() => setPayMethod('WALLET')}
              />
              Wallet
            </label>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                name="pay-method"
                checked={payMethod === 'CARD'}
                onChange={() => setPayMethod('CARD')}
              />
              Test card (does not charge a bank)
            </label>
          </fieldset>
          <Button
            type="button"
            pending={pay.isPending}
            onClick={async () => {
              setPayError(null)
              try {
                await pay.mutateAsync()
              } catch (error) {
                setPayError(
                  isPaymentFailedError(error)
                    ? error.message
                    : errorMessage(error, 'Unable to capture payment.'),
                )
              }
            }}
          >
            Pay now
          </Button>
          <Link to="/wallet" className="ml-3 text-sm font-semibold text-saffron">
            Add money
          </Link>
        </div>
      ) : null}

      {CANCELLABLE_STATUSES.includes(order.status) ? (
        <div>
          {cancel.isError ? <Alert>{errorMessage(cancel.error)}</Alert> : null}
          <Button type="button" variant="ghost" pending={cancel.isPending} onClick={() => cancel.mutate()}>
            Cancel order
          </Button>
        </div>
      ) : null}

      {order.status !== 'CREATED' && order.status !== 'CANCELLED' ? (
        <Link to={`/complaints/new?orderId=${order.id}`} className="inline-block font-semibold text-saffron">
          File a complaint
        </Link>
      ) : null}
    </article>
  )
}

function lifecycleIndex(status: string) {
  const index = LIFECYCLE.indexOf(status as (typeof LIFECYCLE)[number])
  return index === -1 ? -1 : index
}
