import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { supportApi } from '@/api/supportApi'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { Spinner } from '@/components/ui/Spinner'
import { errorMessage } from '@/utils/errors'
import { formatDateTime } from '@/utils/status'


const schema = z.object({
  subject: z.string().trim().min(3, 'Subject is required').max(160),
  body: z.string().trim().min(10, 'Use at least 10 characters').max(2000),
})

type FormValues = z.infer<typeof schema>

export function SupportPage() {
  const queryClient = useQueryClient()
  const list = useQuery({ queryKey: ['support'], queryFn: () => supportApi.list(), retry: false })
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { subject: '', body: '' },
  })
  const create = useMutation({
    mutationFn: (values: FormValues) => supportApi.create(values),
    onSuccess: () => {
      form.reset()
      void queryClient.invalidateQueries({ queryKey: ['support'] })
    },
  })

  return (
    <section className="mx-auto max-w-2xl space-y-8">
      <div>
        <Link to="/help" className="text-sm font-semibold text-saffron">
          Help
        </Link>
        <h1 className="font-display mt-2 text-4xl">Talk to support</h1>
        <p className="mt-2 text-ink/65">
          Write to us about your account or an order. We keep the message on your account for the ops team.
        </p>
      </div>
      <form
        className="space-y-3 rounded-2xl border border-ink/10 bg-white p-5"
        onSubmit={form.handleSubmit((values) => create.mutate(values))}
        noValidate
      >
        {create.isError ? <Alert>{errorMessage(create.error)}</Alert> : null}
        {create.isSuccess ? <Alert tone="success">Message sent. We’ll keep it on your account.</Alert> : null}
        <Field label="Subject" {...form.register('subject')} error={form.formState.errors.subject?.message} />
        <label className="block space-y-1.5">
          <span className="text-sm font-medium">Message</span>
          <textarea
            className="w-full rounded-xl border border-ink/15 px-3.5 py-2.5"
            rows={5}
            {...form.register('body')}
          />
          {form.formState.errors.body?.message ? (
            <span className="text-xs text-red-700">{form.formState.errors.body.message}</span>
          ) : null}
        </label>
        <Button type="submit" pending={create.isPending}>
          Send message
        </Button>
      </form>
      <div>
        <h2 className="font-display text-2xl">Your messages</h2>
        {list.isLoading ? <Spinner label="Loading messages…" /> : null}
        {list.isError ? <Alert>{errorMessage(list.error)}</Alert> : null}
        {list.data?.content.length === 0 ? <Alert tone="info">No messages yet.</Alert> : null}
        <ul className="mt-3 space-y-3">
          {list.data?.content.map((row) => (
            <li key={row.id} className="rounded-2xl border border-ink/10 bg-white px-4 py-3">
              <p className="font-medium">{row.subject}</p>
              <p className="text-xs text-ink/50">
                  {row.status === 'CLOSED' ? 'Closed' : 'Open'} · {formatDateTime(row.createdAt)}
              </p>
              <p className="mt-2 text-sm text-ink/70">{row.body}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
