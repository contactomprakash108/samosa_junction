import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { productApi } from '@/api/productApi'
import { ProductCard } from '@/components/product/ProductCard'
import { Alert } from '@/components/ui/Alert'
import { Field } from '@/components/ui/Field'
import { SelectField } from '@/components/ui/SelectField'
import { Spinner } from '@/components/ui/Spinner'
import type { SpiceLevel } from '@/types/product'
import { errorMessage } from '@/utils/errors'


export function MenuPage() {
  const [params, setParams] = useSearchParams()
  const search = params.get('search') ?? ''
  const category = params.get('category') ?? ''
  const spiceLevel = (params.get('spiceLevel') ?? '') as SpiceLevel | ''
  const page = Number(params.get('page') ?? '0')

  const categoriesQuery = useQuery({
    queryKey: ['categories'],
    queryFn: productApi.categories,
    retry: false,
  })
  const productsQuery = useQuery({
    queryKey: ['products', { search, category, spiceLevel, page }],
    queryFn: () =>
      productApi.list({
        search: search || undefined,
        category: category || undefined,
        spiceLevel: spiceLevel || undefined,
        available: true,
        page,
        size: 9,
        sort: 'name',
      }),
    retry: false,
  })

  function patch(next: Record<string, string>) {
    const merged = new URLSearchParams(params)
    for (const [key, value] of Object.entries(next)) {
      if (value) {
        merged.set(key, value)
      } else {
        merged.delete(key)
      }
    }
    if (!('page' in next)) {
      merged.delete('page')
    }
    setParams(merged)
  }

  return (
    <section className="space-y-6">
      <h1 className="font-display text-3xl">Menu</h1>
      <form
        className="grid gap-3 md:grid-cols-4"
        onSubmit={(event) => {
          event.preventDefault()
          const form = new FormData(event.currentTarget)
          patch({
            search: String(form.get('search') ?? ''),
            category: String(form.get('category') ?? ''),
            spiceLevel: String(form.get('spiceLevel') ?? ''),
          })
        }}
      >
        <Field label="Search" name="search" defaultValue={search} placeholder="Paneer, millet…" />
        <SelectField label="Category" name="category" defaultValue={category}>
          <option value="">All</option>
          {categoriesQuery.data?.map((item) => (
            <option key={item.code} value={item.code}>
              {item.name}
            </option>
          ))}
        </SelectField>
        <SelectField label="Spice" name="spiceLevel" defaultValue={spiceLevel}>
          <option value="">Any</option>
          <option value="MILD">Mild</option>
          <option value="MEDIUM">Medium</option>
          <option value="HOT">Hot</option>
        </SelectField>
        <button type="submit" className="rounded-full bg-ink px-4 py-2 text-sm font-semibold text-cream md:col-span-4">
          Apply filters
        </button>
      </form>

      {productsQuery.isLoading ? <Spinner label="Loading products…" /> : null}
      {productsQuery.isError ? <Alert>{errorMessage(productsQuery.error)}</Alert> : null}
      {productsQuery.data?.content.length === 0 ? <Alert tone="info">No products match those filters.</Alert> : null}
      {productsQuery.data ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {productsQuery.data.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
          <div className="flex justify-between text-sm">
            <button
              type="button"
              disabled={productsQuery.data.first}
              className="disabled:opacity-40"
              onClick={() => patch({ page: String(page - 1) })}
            >
              Previous
            </button>
            <span>
              Page {productsQuery.data.page + 1} of {Math.max(productsQuery.data.totalPages, 1)}
            </span>
            <button
              type="button"
              disabled={productsQuery.data.last}
              className="disabled:opacity-40"
              onClick={() => patch({ page: String(page + 1) })}
            >
              Next
            </button>
          </div>
        </>
      ) : null}
    </section>
  )
}
