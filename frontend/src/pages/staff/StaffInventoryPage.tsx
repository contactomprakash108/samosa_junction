import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { productApi } from '@/api/productApi'
import { staffApi, type StaffInventoryItem } from '@/api/staffApi'
import { errorMessage } from '@/utils/errors'


export function StaffInventoryPage() {
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['staff', 'inventory'],
    queryFn: staffApi.inventory,
    retry: false,
  })
  const update = useMutation({
    mutationFn: ({ productId, quantity }: { productId: string; quantity: number }) =>
      staffApi.setStock(productId, quantity),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['staff', 'inventory'] }),
  })

  return (
    <section className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">Inventory</h1>
      </div>
      {query.isLoading ? <p className="text-zinc-400">Loading stock…</p> : null}
      {query.isError ? <p className="text-red-400">{errorMessage(query.error)}</p> : null}
      {update.isError ? <p className="text-red-400">{errorMessage(update.error)}</p> : null}
      <div className="overflow-x-auto rounded-lg border border-zinc-800">
        <table className="w-full text-left text-sm">
          <thead className="bg-zinc-900 text-zinc-400">
            <tr>
              <th className="px-3 py-2 font-medium">Product</th>
              <th className="px-3 py-2 font-medium">Stock</th>
              <th className="px-3 py-2 font-medium">Flag</th>
              <th className="px-3 py-2 font-medium">Menu</th>
              <th className="px-3 py-2 font-medium">Set qty</th>
            </tr>
          </thead>
          <tbody>
            {query.data?.map((row) => (
              <InventoryRow key={row.productId} row={row} pending={update.isPending} onSave={(quantity) => update.mutate({ productId: row.productId, quantity })} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function InventoryRow({
  row,
  pending,
  onSave,
}: {
  row: StaffInventoryItem
  pending: boolean
  onSave: (quantity: number) => void
}) {
  const queryClient = useQueryClient()
  const [value, setValue] = useState(String(row.quantity))
  const toggle = useMutation({
    mutationFn: async () => {
      const product = await productApi.getById(row.productId)
      return productApi.update(row.productId, {
        name: product.name,
        description: product.description,
        category: product.category,
        price: product.price,
        ingredients: product.ingredients,
        calories: product.calories,
        protein: product.protein,
        spiceLevel: product.spiceLevel,
        dietaryTags: product.dietaryTags,
        available: !product.available,
      })
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['staff', 'inventory'] })
      void queryClient.invalidateQueries({ queryKey: ['products'] })
      void queryClient.invalidateQueries({ queryKey: ['staff', 'products'] })
    },
  })

  return (
    <tr className="border-t border-zinc-800">
      <td className="px-3 py-2">{row.name}</td>
      <td className="px-3 py-2 tabular-nums">{row.quantity}</td>
      <td className="px-3 py-2">
        {row.soldOut ? <span className="text-red-400">Sold out</span> : null}
        {row.lowStock ? <span className="text-amber-400">Low</span> : null}
        {!row.soldOut && !row.lowStock ? <span className="text-zinc-500">OK</span> : null}
      </td>
      <td className="px-3 py-2">
        <button
          type="button"
          className="text-amber-400 hover:underline"
          disabled={toggle.isPending}
          onClick={() => toggle.mutate()}
        >
          {row.available ? 'Hide from menu' : 'Show on menu'}
        </button>
        <p className="text-xs text-zinc-500">{row.available ? 'On menu' : 'Hidden'}</p>
        {toggle.isError ? <p className="text-xs text-red-400">{errorMessage(toggle.error)}</p> : null}
      </td>
      <td className="px-3 py-2">
        <form
          className="flex gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            onSave(Number(value))
          }}
        >
          <input
            className="w-20 rounded border border-zinc-700 bg-zinc-950 px-2 py-1"
            value={value}
            onChange={(event) => setValue(event.target.value)}
            inputMode="numeric"
          />
          <button type="submit" disabled={pending} className="rounded bg-zinc-800 px-2 py-1 text-xs">
            Save
          </button>
        </form>
      </td>
    </tr>
  )
}

