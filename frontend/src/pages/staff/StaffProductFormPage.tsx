import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { productApi } from '@/api/productApi'
import type { ProductWrite, SpiceLevel } from '@/types/product'
import { errorMessage } from '@/utils/errors'


const empty: ProductWrite = {
  name: '',
  description: '',
  category: 'CLASSIC',
  price: 40,
  ingredients: ['potato'],
  calories: 250,
  protein: 6,
  spiceLevel: 'MILD',
  dietaryTags: ['VEGETARIAN'],
  available: true,
}

export function StaffProductFormPage() {
  const { productId } = useParams()
  const isNew = !productId
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<ProductWrite>(empty)
  const [file, setFile] = useState<File | null>(null)
  const categories = useQuery({ queryKey: ['categories'], queryFn: productApi.categories })
  const existing = useQuery({
    queryKey: ['staff', 'product', productId],
    queryFn: () => productApi.getById(productId!),
    enabled: !isNew,
    retry: false,
  })

  useEffect(() => {
    if (existing.data) {
      setForm({
        name: existing.data.name,
        description: existing.data.description,
        category: existing.data.category,
        price: existing.data.price,
        ingredients: existing.data.ingredients,
        calories: existing.data.calories,
        protein: existing.data.protein,
        spiceLevel: existing.data.spiceLevel,
        dietaryTags: existing.data.dietaryTags,
        available: existing.data.available,
      })
    }
  }, [existing.data])

  const save = useMutation({
    mutationFn: async () => {
      const saved = isNew ? await productApi.create(form) : await productApi.update(productId!, form)
      if (file) {
        await productApi.uploadImage(saved.id, file)
      }
      return saved
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['staff', 'products'] })
      await queryClient.invalidateQueries({ queryKey: ['products'] })
      navigate('/staff/products')
    },
  })

  if (!isNew && existing.isLoading) {
    return <p className="text-zinc-400">Loading product…</p>
  }

  return (
    <section className="max-w-xl space-y-4">
      <Link to="/staff/products" className="text-sm text-amber-400">
        Products
      </Link>
      <h1 className="text-2xl font-semibold">{isNew ? 'New product' : 'Edit product'}</h1>
      <form
        className="space-y-3"
        onSubmit={(event) => {
          event.preventDefault()
          save.mutate()
        }}
      >
        {save.isError ? <p className="text-red-400">{errorMessage(save.error)}</p> : null}
        <Field label="Name" value={form.name} onChange={(name) => setForm({ ...form, name })} />
        <label className="block text-sm">
          Description
          <textarea
            className="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-3 py-2"
            rows={3}
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
          />
        </label>
        <label className="block text-sm">
          Category
          <select
            className="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-3 py-2"
            value={form.category}
            onChange={(event) => setForm({ ...form, category: event.target.value })}
          >
            {(categories.data ?? [{ code: form.category, name: form.category }]).map((category) => (
              <option key={category.code} value={category.code}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
        <Field label="Price (₹)" value={String(form.price)} onChange={(price) => setForm({ ...form, price: Number(price) })} />
        <Field
          label="Ingredients (comma)"
          value={form.ingredients.join(', ')}
          onChange={(value) => setForm({ ...form, ingredients: split(value) })}
        />
        <Field label="Calories" value={String(form.calories)} onChange={(calories) => setForm({ ...form, calories: Number(calories) })} />
        <Field label="Protein" value={String(form.protein)} onChange={(protein) => setForm({ ...form, protein: Number(protein) })} />
        <label className="block text-sm">
          Spice
          <select
            className="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-3 py-2"
            value={form.spiceLevel}
            onChange={(event) => setForm({ ...form, spiceLevel: event.target.value as SpiceLevel })}
          >
            <option value="MILD">Mild</option>
            <option value="MEDIUM">Medium</option>
            <option value="HOT">Hot</option>
          </select>
        </label>
        <Field
          label="Dietary tags (comma)"
          value={form.dietaryTags.join(', ')}
          onChange={(value) => setForm({ ...form, dietaryTags: split(value) })}
        />
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={form.available}
            onChange={(event) => setForm({ ...form, available: event.target.checked })}
          />
          On the customer menu (uncheck to hide; sold-out is stock 0)
        </label>
        <label className="block text-sm">
          Product photo
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="mt-1 block w-full text-zinc-400"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <span className="mt-1 block text-xs text-zinc-500">
            Optional JPEG, PNG, or WebP. Same as Add image on an existing product.
          </span>
        </label>
        <button type="submit" disabled={save.isPending} className="rounded-md bg-amber-500 px-4 py-2 font-semibold text-zinc-950">
          Save
        </button>
      </form>
    </section>
  )
}

function Field({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block text-sm">
      {label}
      <input
        className="mt-1 w-full rounded border border-zinc-700 bg-zinc-950 px-3 py-2"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  )
}

function split(value: string) {
  return value
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}
