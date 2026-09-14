import { Link } from 'react-router-dom'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Spinner } from '@/components/ui/Spinner'
import { useCart } from '@/hooks/useCart'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'


export function CartPage() {
  const { query, update, remove, clear } = useCart()

  if (query.isLoading) {
    return <Spinner label="Loading cart…" />
  }
  if (query.isError) {
    return <Alert>{errorMessage(query.error, 'Unable to load your cart.')}</Alert>
  }

  const cart = query.data
  if (!cart || cart.items.length === 0) {
    return (
      <section className="mx-auto max-w-lg rounded-[2rem] border border-ink/10 bg-white px-8 py-12 text-center shadow-sm">
        <h1 className="font-display text-3xl">Your tray is empty</h1>
        <p className="mt-3 text-ink/65">Add a samosa from the menu and we’ll keep it ready for checkout.</p>
        <Link
          to="/"
          className="mt-6 inline-flex rounded-full bg-saffron px-5 py-2.5 text-sm font-semibold text-cream hover:bg-saffron-dark"
        >
          Browse the menu
        </Link>
      </section>
    )
  }

  const blocked = cart.items.some((item) => !item.available || item.productMissing)

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="font-display text-3xl">Cart</h1>
        <Button type="button" variant="ghost" pending={clear.isPending} onClick={() => clear.mutate()}>
          Empty cart
        </Button>
      </div>
      {blocked ? (
        <Alert>Some items are no longer available. Remove them before checkout.</Alert>
      ) : null}
      <ul className="divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
        {cart.items.map((item) => (
          <li key={item.productId} className="flex flex-wrap items-center gap-4 px-4 py-4">
            <div className="min-w-48 flex-1">
              <p className="font-medium">{item.name}</p>
              <p className="text-sm text-ink/55">
                {formatInr(item.unitPrice)} each
                {item.stock > 0 ? ` · ${item.stock} in stock` : ''}
              </p>
              {!item.available || item.productMissing ? (
                <p className="text-xs text-red-700">Not available</p>
              ) : null}
            </div>
            <label className="text-sm">
              Qty{' '}
              <input
                type="number"
                min={0}
                max={Math.max(0, item.stock)}
                defaultValue={item.quantity}
                className="w-16 rounded-lg border border-ink/15 px-2 py-1"
                onBlur={(event) => {
                  const quantity = Number(event.target.value)
                  if (quantity === item.quantity) {
                    return
                  }
                  void update.mutateAsync({ productId: item.productId, quantity })
                }}
              />
            </label>
            <p className="w-24 text-right font-semibold">{formatInr(item.lineTotal)}</p>
            <button
              type="button"
              className="text-sm text-saffron"
              onClick={() => remove.mutate(item.productId)}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <p className="text-lg font-semibold">
          Estimated total {formatInr(cart.subtotal)}{' '}
          <span className="text-sm font-normal text-ink/55">({cart.totalQuantity} items)</span>
        </p>
        <Link
          to="/checkout"
          className={`rounded-full bg-saffron px-5 py-2.5 text-sm font-semibold text-cream ${
            blocked ? 'pointer-events-none opacity-50' : ''
          }`}
        >
          Checkout
        </Link>
      </div>
    </section>
  )
}
