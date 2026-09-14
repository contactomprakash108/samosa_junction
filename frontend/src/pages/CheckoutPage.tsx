import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { Spinner } from '@/components/ui/Spinner'
import { useCart } from '@/hooks/useCart'
import { useWallet } from '@/hooks/useWallet'
import { useAuth } from '@/hooks/useAuth'
import { useQueryClient } from '@tanstack/react-query'
import { orderApi } from '@/api/orderApi'
import type { DeliveryAddress } from '@/types/order'
import { newIdempotencyKey } from '@/utils/idempotency'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'


const addressSchema = z.object({
  recipientName: z.string().trim().min(1, 'Name is required').max(120),
  line1: z.string().trim().min(1, 'Address is required').max(200),
  city: z.string().trim().min(1, 'City is required').max(80),
  state: z.string().trim().min(1, 'State is required').max(80),
  pincode: z.string().trim().min(4, 'Pincode is required').max(16),
})

type AddressValues = z.infer<typeof addressSchema>
type Step = 'address' | 'review'

export function CheckoutPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { query: cartQuery, resetAfterCheckout } = useCart()
  const walletQuery = useWallet()
  const [step, setStep] = useState<Step>('address')
  const [address, setAddress] = useState<DeliveryAddress | null>(null)
  const [paymentMethod, setPaymentMethod] = useState<'WALLET' | 'CARD' | 'COD'>('WALLET')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const orderKey = useMemo(() => newIdempotencyKey(), [])

  const { user } = useAuth()
  const form = useForm<AddressValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      recipientName: user?.recipientName || user?.fullName || '',
      line1: user?.addressLine1 || '',
      city: user?.city || '',
      state: user?.state || '',
      pincode: user?.pincode || '',
    },
  })

  useEffect(() => {
    if (!user) {
      return
    }
    form.reset({
      recipientName: user.recipientName || user.fullName || '',
      line1: user.addressLine1 || '',
      city: user.city || '',
      state: user.state || '',
      pincode: user.pincode || '',
    })
  }, [user, form])

  if (cartQuery.isLoading) {
    return <Spinner label="Loading checkout…" />
  }
  if (cartQuery.isError) {
    return <Alert>{errorMessage(cartQuery.error)}</Alert>
  }
  const cart = cartQuery.data
  if (!cart || cart.items.length === 0) {
    return (
      <section className="space-y-4">
        <h1 className="font-display text-3xl">Checkout</h1>
        <Alert tone="info">Your cart is empty.</Alert>
        <Link to="/" className="font-semibold text-saffron">
          Browse snacks
        </Link>
      </section>
    )
  }

  async function placeOrder() {
    if (!address) {
      return
    }
    setPending(true)
    setError(null)
    try {
      const order = await orderApi.create(address, paymentMethod !== 'COD', orderKey, paymentMethod)
      await resetAfterCheckout()
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
      await queryClient.invalidateQueries({ queryKey: ['orders', 'complaint-picker'] })
      await queryClient.invalidateQueries({ queryKey: ['wallet'] })
      await queryClient.invalidateQueries({ queryKey: ['users', 'me'] })
      navigate(`/orders/${order.id}`, { replace: true, state: { justPlaced: true } })
    } catch (cause) {
      setError(errorMessage(cause, 'Unable to place the order. Please try again.'))
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="mx-auto max-w-2xl space-y-6">
      <h1 className="font-display text-3xl">Checkout</h1>
      <ol className="flex flex-wrap gap-2 text-xs font-semibold tracking-wide uppercase">
        <li className={step === 'address' ? 'text-saffron' : 'text-ink/40'}>1 Address</li>
        <li className={step === 'review' ? 'text-saffron' : 'text-ink/40'}>2 Summary</li>
        <li className="text-ink/40">3 Confirmation</li>
      </ol>

      {step === 'address' ? (
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit((values) => {
            setAddress(values)
            setStep('review')
          })}
          noValidate
        >
          <p className="text-sm text-ink/60">
            Prefills from your Profile default address. Saving an order also updates that default.
          </p>
          <Field label="Recipient" {...form.register('recipientName')} error={form.formState.errors.recipientName?.message} />
          <Field label="Address line" {...form.register('line1')} error={form.formState.errors.line1?.message} />
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="City" {...form.register('city')} error={form.formState.errors.city?.message} />
            <Field label="State" {...form.register('state')} error={form.formState.errors.state?.message} />
            <Field label="Pincode" {...form.register('pincode')} error={form.formState.errors.pincode?.message} />
          </div>
          <Button type="submit">Continue to summary</Button>
        </form>
      ) : (
        <div className="space-y-4">
          {error ? <Alert>{error}</Alert> : null}
          <div className="rounded-2xl border border-ink/10 bg-white p-4">
            <h2 className="font-semibold">Delivery</h2>
            <p className="mt-1 text-sm text-ink/70">
              {address?.recipientName}
              <br />
              {address?.line1}, {address?.city}, {address?.state} {address?.pincode}
            </p>
          </div>
          <ul className="divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
            {cart.items.map((item) => (
              <li key={item.productId} className="flex justify-between px-4 py-3 text-sm">
                <span>
                  {item.quantity} × {item.name}
                </span>
                <span>{formatInr(item.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <p className="text-lg font-semibold">
            Estimated total {formatInr(cart.subtotal)}
            <span className="block text-sm font-normal text-ink/55">
              We’ll confirm the total when you place the order.
            </span>
          </p>
          <p className="text-sm text-ink/70">
            Wallet balance:{' '}
            {walletQuery.data ? formatInr(walletQuery.data.balance) : walletQuery.isError ? 'unavailable' : '…'}
          </p>
          <fieldset className="space-y-2 text-sm">
            <legend className="font-medium">Pay with</legend>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                name="method"
                checked={paymentMethod === 'WALLET'}
                onChange={() => setPaymentMethod('WALLET')}
              />
              Wallet
            </label>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                name="method"
                checked={paymentMethod === 'CARD'}
                onChange={() => setPaymentMethod('CARD')}
              />
              Test card (does not charge a bank)
            </label>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                name="method"
                checked={paymentMethod === 'COD'}
                onChange={() => setPaymentMethod('COD')}
              />
              Cash on delivery
            </label>
          </fieldset>
          {paymentMethod === 'COD' ? (
            <p className="text-sm text-ink/65">
              Kitchen starts this order now. Pay the rider in cash. Staff marks it paid after delivery.
            </p>
          ) : null}
          <div className="flex flex-wrap gap-3">
            <Button type="button" pending={pending} onClick={() => void placeOrder()}>
              Confirm order
            </Button>
            <Button type="button" variant="ghost" onClick={() => setStep('address')}>
              Edit address
            </Button>
          </div>
        </div>
      )}
    </section>
  )
}
