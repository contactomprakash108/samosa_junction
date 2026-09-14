import { Link } from 'react-router-dom'
import { CompactAddToCart } from '@/components/cart/CompactAddToCart'
import { ProductPhoto } from '@/components/product/ProductPhoto'
import type { Product } from '@/types/product'
import { categoryLabel, proteinLabel, spiceLabel } from '@/utils/catalog'
import { formatInr } from '@/utils/money'


export function ProductCard({ product }: { product: Product }) {
  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-ink/10 bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-[0_18px_40px_-24px_rgba(28,21,16,0.5)]">
      <Link to={`/products/${product.id}`} className="relative aspect-[5/4] overflow-hidden bg-cream-dark">
        <ProductPhoto
          product={product}
          className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
        />
        <span className="absolute top-3 left-3 rounded-full bg-cream/95 px-2.5 py-1 text-xs font-semibold text-saffron">
          {categoryLabel(product.category)}
        </span>
        {!product.available || product.stock <= 0 ? (
          <span className="absolute top-3 right-3 rounded-full bg-ink/80 px-2.5 py-1 text-xs font-semibold text-cream">
            {product.stock <= 0 ? 'Sold out' : 'Unavailable'}
          </span>
        ) : null}
      </Link>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <h3 className="font-display text-xl leading-tight text-ink">{product.name}</h3>
        <p className="line-clamp-2 text-sm text-ink/65">{product.description}</p>
        <p className="text-xs text-ink/50">
          {product.calories} kcal · {proteinLabel(product.protein)} · {spiceLabel(product.spiceLevel)}
        </p>
        <div className="mt-auto flex items-end justify-between gap-3 pt-3">
          <div>
            <p className="text-lg font-semibold text-ink">{formatInr(product.price)}</p>
            <Link to={`/products/${product.id}`} className="text-xs font-semibold text-saffron hover:text-saffron-dark">
              View details
            </Link>
          </div>
          <CompactAddToCart productId={product.id} available={product.available && product.stock > 0} stock={product.stock} />
        </div>
      </div>
    </article>
  )
}
