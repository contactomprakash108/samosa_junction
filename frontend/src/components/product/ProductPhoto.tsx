import { useState } from 'react'
import type { Product } from '@/types/product'
import { productPhoto } from '@/utils/catalog'


export function ProductPhoto({
  product,
  className = 'h-full w-full object-cover',
}: {
  product: Pick<Product, 'name' | 'imageUrl'>
  className?: string
}) {
  const src = productPhoto(product)
  const [failed, setFailed] = useState(false)

  if (!src || failed) {
    return (
      <div className="flex h-full min-h-40 items-center justify-center bg-cream-dark px-4 text-center text-sm text-ink/50">
        {product.name}
      </div>
    )
  }

  return <img src={src} alt={product.name} className={className} onError={() => setFailed(true)} />
}
