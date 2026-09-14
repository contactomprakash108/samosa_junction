import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { productApi } from '@/api/productApi'
import { AddToCartButton } from '@/components/cart/AddToCartButton'
import { ProductPhoto } from '@/components/product/ProductPhoto'
import { Alert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { categoryLabel, proteinLabel, spiceLabel } from '@/utils/catalog'
import { errorMessage } from '@/utils/errors'
import { formatInr } from '@/utils/money'
import { formatEnum } from '@/utils/status'


export function ProductDetailPage() {
  const { productId } = useParams()
  const query = useQuery({
    queryKey: ['products', productId],
    queryFn: () => productApi.getById(productId!),
    enabled: Boolean(productId),
    retry: false,
  })

  if (query.isLoading) {
    return <Spinner label="Loading product…" />
  }
  if (query.isError || !query.data) {
    return (
      <div className="space-y-4">
        <Alert>{errorMessage(query.error, 'Unable to load this product.')}</Alert>
        <Link to="/" className="text-sm font-semibold text-saffron">
          Back to the menu
        </Link>
      </div>
    )
  }

  const product = query.data

  return (
    <article className="grid gap-8 overflow-hidden rounded-[2rem] border border-ink/10 bg-white p-4 shadow-sm md:grid-cols-2 md:p-6">
      <div className="min-h-80 overflow-hidden rounded-[1.25rem] bg-cream-dark">
        <ProductPhoto product={product} className="h-full min-h-80 w-full object-cover" />
      </div>
      <div className="space-y-4 md:py-4">
        <p className="text-xs font-semibold tracking-wide text-saffron uppercase">{categoryLabel(product.category)}</p>
        <h1 className="font-display text-4xl">{product.name}</h1>
        <p className="text-ink/70">{product.description}</p>
        <p className="text-2xl font-semibold">{formatInr(product.price)}</p>
        <p className="text-sm text-ink/60">
          {product.stock <= 0 ? 'Sold out.' : product.available ? 'Ready to fold into your order.' : 'Currently unavailable.'}
        </p>
        <p className="text-sm text-ink/55">
          {product.calories} kcal · {proteinLabel(product.protein)} · {spiceLabel(product.spiceLevel)}
        </p>
        <dl className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <dt className="text-ink/50">Calories</dt>
            <dd>{product.calories}</dd>
          </div>
          <div>
            <dt className="text-ink/50">Protein</dt>
            <dd>{proteinLabel(product.protein)}</dd>
          </div>
          <div>
            <dt className="text-ink/50">Spice</dt>
            <dd className="capitalize">{spiceLabel(product.spiceLevel)}</dd>
          </div>
        </dl>
        <div>
          <h2 className="text-sm font-semibold">Ingredients</h2>
          <p className="text-sm text-ink/70">{product.ingredients.join(', ')}</p>
        </div>
        <div>
          <h2 className="text-sm font-semibold">Dietary tags</h2>
          <p className="text-sm text-ink/70">{product.dietaryTags.map(formatEnum).join(', ')}</p>
        </div>
        <AddToCartButton productId={product.id} available={product.available && product.stock > 0} stock={product.stock} />
      </div>
    </article>
  )
}
