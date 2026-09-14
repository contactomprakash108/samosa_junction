import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router-dom'
import { userApi } from '@/api/userApi'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { useAuth } from '@/hooks/useAuth'
import { errorMessage } from '@/utils/errors'
import { useState } from 'react'


const schema = z.object({
  fullName: z.string().trim().min(1, 'Name is required').max(120),
  phone: z.string().trim().max(20),
  recipientName: z.string().trim().min(1, 'Recipient is required').max(120),
  addressLine1: z.string().trim().min(1, 'Address is required').max(200),
  city: z.string().trim().min(1, 'City is required').max(80),
  state: z.string().trim().min(1, 'State is required').max(80),
  pincode: z.string().trim().min(4, 'Pincode is required').max(16),
})

type Values = z.infer<typeof schema>

export function ProfilePage() {
  const { user, logout } = useAuth()
  const queryClient = useQueryClient()
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    values: user
      ? {
          fullName: user.fullName ?? '',
          phone: user.phone ?? '',
          recipientName: user.recipientName ?? user.fullName ?? '',
          addressLine1: user.addressLine1 ?? '',
          city: user.city ?? '',
          state: user.state ?? '',
          pincode: user.pincode ?? '',
        }
      : undefined,
  })

  const mutation = useMutation({
    mutationFn: userApi.update,
    onSuccess(next) {
      queryClient.setQueryData(['users', 'me'], next)
      setSaved(true)
      setError(null)
    },
    onError(cause) {
      setSaved(false)
      setError(errorMessage(cause, 'Could not save your profile.'))
    },
  })

  if (!user) {
    return null
  }

  return (
    <section className="mx-auto max-w-2xl space-y-6">
      <div className="rounded-[2rem] bg-ink px-6 py-8 text-cream">
        <p className="text-xs font-semibold tracking-[0.2em] text-gold uppercase">Signed in</p>
        <h1 className="font-display mt-2 text-4xl">{user.fullName}</h1>
        <p className="mt-2 text-cream/70">{user.email}</p>
        <p className="mt-3 text-sm text-cream/60">
          {user.hasDefaultAddress
            ? `Default delivery: ${user.addressLine1}, ${user.city} ${user.pincode}`
            : 'No default address yet — Samosa AI and Checkout use this form.'}
        </p>
      </div>

      <form
        className="space-y-4 rounded-2xl border border-ink/10 bg-white p-5"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        noValidate
      >
        <h2 className="font-display text-2xl">Customer details</h2>
        <p className="text-sm text-ink/60">
          Phone and default address are what Samosa AI uses when you say “order and pay from wallet.”
        </p>
        <Field label="Full name" {...form.register('fullName')} error={form.formState.errors.fullName?.message} />
        <Field label="Phone" {...form.register('phone')} error={form.formState.errors.phone?.message} />
        <Field
          label="Delivery name"
          {...form.register('recipientName')}
          error={form.formState.errors.recipientName?.message}
        />
        <Field
          label="Address line"
          {...form.register('addressLine1')}
          error={form.formState.errors.addressLine1?.message}
        />
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="City" {...form.register('city')} error={form.formState.errors.city?.message} />
          <Field label="State" {...form.register('state')} error={form.formState.errors.state?.message} />
          <Field label="Pincode" {...form.register('pincode')} error={form.formState.errors.pincode?.message} />
        </div>
        {error ? <p className="text-sm text-red-700">{error}</p> : null}
        {saved ? <p className="text-sm text-leaf">Saved. AI checkout will ship here.</p> : null}
        <Button type="submit" pending={mutation.isPending}>
          Save default address
        </Button>
      </form>

      <dl className="divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
        <Row label="Roles" value={user.roles.join(', ')} />
        <Row label="Member since" value={new Date(user.createdAt).toLocaleDateString('en-IN')} />
      </dl>
      <div className="grid gap-3 sm:grid-cols-2">
        <ProfileLink to="/wallet" label="Wallet" hint="Add money and pay for orders" />
        <ProfileLink to="/orders" label="Orders" hint="Pay, track, or cancel" />
        <ProfileLink to="/cart" label="Cart" hint="Items waiting at checkout" />
        <ProfileLink to="/help" label="Help" hint="Guides, support, or a complaint" />
        <ProfileLink to="/assistant" label="Ask Samosa AI" hint="Tell us what you’re craving" />
        <ProfileLink to="/complaints" label="Complaints" hint="Tickets on paid orders" />
        {user.roles.some((role) => role === 'STAFF' || role === 'ADMIN') ? (
          <ProfileLink to="/staff" label="Kitchen ops" hint="Queue, stock, complaints" />
        ) : null}
      </div>
      <button
        type="button"
        onClick={logout}
        className="rounded-full border border-ink/15 px-4 py-2 text-sm font-semibold text-ink hover:bg-white"
      >
        Log out
      </button>
    </section>
  )
}

function ProfileLink({ to, label, hint }: { to: string; label: string; hint: string }) {
  return (
    <Link to={to} className="rounded-2xl border border-ink/10 bg-white px-4 py-4 shadow-sm hover:border-saffron/40">
      <p className="font-semibold text-ink">{label}</p>
      <p className="text-sm text-ink/55">{hint}</p>
    </Link>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4 px-4 py-3 text-sm">
      <dt className="text-ink/50">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  )
}
