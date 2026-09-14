import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { useCart } from '@/hooks/useCart'
import { errorMessage } from '@/utils/errors'


export function CompactAddToCart({
  productId,
  available,
  stock,
}: {
  productId: string
  available: boolean
  stock: number
}) {
  const { status } = useAuth()
  const { query, add, update, remove } = useCart()
  const [error, setError] = useState<string | null>(null)
  const line = query.data?.items.find((item) => item.productId === productId)
  const pending = add.isPending || update.isPending || remove.isPending

  if (!available) {
    return <p className="text-sm font-medium text-ink/50">Sold out</p>
  }

  if (status !== 'authenticated') {
    return (
      <Link
        to="/login"
        className="inline-flex min-h-10 items-center rounded-full bg-saffron px-4 text-sm font-semibold text-cream hover:bg-saffron-dark"
      >
        + Add
      </Link>
    )
  }

  if (line) {
    return (
      <div className="space-y-1">
        <div className="inline-flex items-center rounded-full bg-ink text-cream">
          <button
            type="button"
            className="min-h-10 min-w-10 text-lg"
            disabled={pending}
            aria-label="Decrease quantity"
            onClick={() => {
              setError(null)
              const next = line.quantity - 1
              if (next <= 0) {
                remove.mutate(productId, { onError: (err) => setError(errorMessage(err, 'Unable to update cart.')) })
                return
              }
              update.mutate(
                { productId, quantity: next },
                { onError: (err) => setError(errorMessage(err, 'Unable to update cart.')) },
              )
            }}
          >
            −
          </button>
          <span className="min-w-6 text-center text-sm font-semibold">{line.quantity}</span>
          <button
            type="button"
            className="min-h-10 min-w-10 text-lg"
            disabled={pending || line.quantity >= Math.min(20, stock)}
            aria-label="Increase quantity"
            onClick={() => {
              setError(null)
              update.mutate(
                { productId, quantity: line.quantity + 1 },
                { onError: (err) => setError(errorMessage(err, 'Unable to update cart.')) },
              )
            }}
          >
            +
          </button>
        </div>
        {error ? <p className="text-xs text-red-700">{error}</p> : null}
      </div>
    )
  }

  return (
    <div className="space-y-1">
      <button
        type="button"
        disabled={pending}
        className="inline-flex min-h-10 items-center rounded-full bg-saffron px-4 text-sm font-semibold text-cream hover:bg-saffron-dark disabled:opacity-60"
        onClick={() => {
          setError(null)
          add.mutate({ productId, quantity: 1 }, { onError: (err) => setError(errorMessage(err, 'Unable to add to cart.')) })
        }}
      >
        {pending ? 'Adding…' : '+ Add'}
      </button>
      {error ? <p className="text-xs text-red-700">{error}</p> : null}
    </div>
  )
}
