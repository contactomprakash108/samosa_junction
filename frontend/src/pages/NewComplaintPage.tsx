import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { complaintApi } from '@/api/complaintApi'
import { orderApi } from '@/api/orderApi'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { SelectField } from '@/components/ui/SelectField'
import { Spinner } from '@/components/ui/Spinner'
import { COMPLAINT_CATEGORIES } from '@/types/complaint'
import { newIdempotencyKey } from '@/utils/idempotency'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'
import { formatDateTime, formatEnum, isPaidOrder } from '@/utils/status'


const schema = z.object({
  orderId: z.string().min(1, 'Select a paid order'),
  category: z.enum(['DAMAGED', 'MISSING_ITEM', 'WRONG_ITEM', 'LATE', 'QUALITY', 'OTHER']),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH']),
  description: z.string().trim().min(10, 'Use at least 10 characters').max(1000),
})

type FormValues = z.infer<typeof schema>

export function NewComplaintPage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const preset = params.get('orderId') ?? ''
  const idempotencyKey = useMemo(() => newIdempotencyKey(), [])
  const ordersQuery = useQuery({
    queryKey: ['orders', 'complaint-picker'],
    queryFn: () => orderApi.list(0, 50),
    retry: false,
  })

  const paid = (ordersQuery.data?.content ?? []).filter((order) => isPaidOrder(order.status))

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      orderId: preset,
      category: 'DAMAGED',
      priority: 'MEDIUM',
      description: '',
    },
  })

  async function onSubmit(values: FormValues) {
    try {
      const created = await complaintApi.create(values, idempotencyKey)
      navigate(`/complaints/${created.id}`, { replace: true })
    } catch (error) {
      form.setError('root', { message: errorMessage(error, 'Unable to submit the complaint.') })
    }
  }

  return (
    <section className="mx-auto max-w-xl space-y-4">
      <h1 className="font-display text-3xl">New complaint</h1>
      <p className="text-sm text-ink/65">
        Pick a paid order. We already have your account email — you do not need to enter email or phone again. An
        executive will call you soon after you submit.
      </p>
      {ordersQuery.isLoading ? <Spinner label="Loading your orders…" /> : null}
      {ordersQuery.isError ? <Alert>{errorMessage(ordersQuery.error, 'Unable to load orders.')}</Alert> : null}
      {!ordersQuery.isLoading && !ordersQuery.isError && paid.length === 0 ? (
        <div className="space-y-2">
          <Alert tone="info">No paid orders yet. Open Orders after a successful checkout.</Alert>
          <Link to="/orders" className="text-sm font-semibold text-saffron">
            View orders
          </Link>
        </div>
      ) : null}
      <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
        {form.formState.errors.root?.message ? <Alert>{form.formState.errors.root.message}</Alert> : null}
        <SelectField label="Order" {...form.register('orderId')} error={form.formState.errors.orderId?.message}>
          <option value="">Select…</option>
          {paid.map((order) => (
            <option key={order.id} value={order.id}>
              {order.items.map((item) => item.name).join(', ') || formatEnum(order.status)} ·{' '}
              {formatEnum(order.status)} · {formatInr(order.total)} · {formatDateTime(order.createdAt)}
            </option>
          ))}
        </SelectField>
        <SelectField label="Type" {...form.register('category')} error={form.formState.errors.category?.message}>
          {COMPLAINT_CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {formatEnum(category)}
            </option>
          ))}
        </SelectField>
        <SelectField label="Priority" {...form.register('priority')} error={form.formState.errors.priority?.message}>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </SelectField>
        <label className="block space-y-1.5">
          <span className="text-sm font-medium">What happened?</span>
          <textarea
            className="min-h-32 w-full rounded-xl border border-ink/15 px-3.5 py-2.5"
            {...form.register('description')}
          />
          {form.formState.errors.description?.message ? (
            <span className="text-xs text-red-700">{form.formState.errors.description.message}</span>
          ) : null}
        </label>
        <Button type="submit" pending={form.formState.isSubmitting} disabled={paid.length === 0}>
          Submit complaint
        </Button>
      </form>
    </section>
  )
}
