import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/hooks/useAuth'
import { useCart } from '@/hooks/useCart'
import { errorMessage } from '@/utils/errors'


export function AddToCartButton({
  productId,
  available,
  stock,
}: {
  productId: string
  available: boolean
  stock: number
}) {
  const { status } = useAuth()
  const { add } = useCart()
  const [quantity, setQuantity] = useState(1)
  const [message, setMessage] = useState<string | null>(null)

  if (status !== 'authenticated') {
    return (
      <p className="text-sm text-ink/65">
        <Link to="/login" className="font-semibold text-saffron">
          Log in
        </Link>{' '}
        to add this to your cart.
      </p>
    )
  }

  if (!available) {
    return <p className="text-sm text-ink/60">This samosa isn’t available right now.</p>
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3">
        <label className="text-sm text-ink/70">
          Qty{' '}
          <input
            type="number"
            min={1}
            max={Math.max(1, stock)}
            value={quantity}
            onChange={(event) => setQuantity(Number(event.target.value))}
            className="ml-1 w-16 rounded-lg border border-ink/15 px-2 py-1"
          />
        </label>
        <Button
          type="button"
          pending={add.isPending}
          onClick={async () => {
            setMessage(null)
            try {
              await add.mutateAsync({ productId, quantity: Math.min(quantity, stock) })
              setMessage('Added to cart.')
            } catch (error) {
              setMessage(errorMessage(error, 'Unable to add to cart.'))
            }
          }}
        >
          Add to cart
        </Button>
      </div>
      {message ? <p className="text-sm text-ink/70">{message}</p> : null}
    </div>
  )
}
