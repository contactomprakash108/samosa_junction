import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { productApi } from '@/api/productApi'
import { errorMessage } from '@/utils/errors'
import { formatInr } from '@/utils/money'


export function StaffProductsPage() {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['staff', 'products'],
    queryFn: () => productApi.list({ size: 100, sort: 'name,asc' }),
    retry: false,
  })
  const remove = useMutation({
    mutationFn: (id: string) => productApi.remove(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['staff', 'products'] }),
  })

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Products</h1>
        <Link to="/staff/products/new" className="rounded-md bg-amber-500 px-3 py-2 text-sm font-semibold text-zinc-950">
          New product
        </Link>
      </div>
      {query.isLoading ? <p className="text-zinc-400">Loading products…</p> : null}
      {query.isError ? <p className="text-red-400">{errorMessage(query.error)}</p> : null}
      {remove.isError ? <p className="text-red-400">{errorMessage(remove.error)}</p> : null}
      <ul className="divide-y divide-zinc-800 rounded-lg border border-zinc-800">
        {query.data?.content.map((product) => (
          <li key={product.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
            <div>
              <p className="font-medium">{product.name}</p>
              <p className="text-xs text-zinc-500">
                {formatInr(product.price)} · {product.available ? 'On menu' : 'Hidden'}
              </p>
            </div>
            <div className="flex gap-3 text-sm">
              <Link to={`/staff/products/${product.id}`} className="text-amber-400">
                Edit
              </Link>
              <button type="button" className="text-red-400" onClick={() => remove.mutate(product.id)}>
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
