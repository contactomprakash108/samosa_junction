import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { walletApi } from '@/api/walletApi'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Field } from '@/components/ui/Field'
import { Spinner } from '@/components/ui/Spinner'
import { walletQueryKey } from '@/hooks/useWallet'
import { newIdempotencyKey } from '@/utils/idempotency'
import { formatInr } from '@/utils/money'
import { errorMessage } from '@/utils/errors'
import { formatDateTime } from '@/utils/status'


export function WalletPage() {
  const queryClient = useQueryClient()
  const walletQuery = useQuery({ queryKey: walletQueryKey, queryFn: walletApi.get, retry: false })
  const txQuery = useQuery({
    queryKey: ['wallet', 'transactions'],
    queryFn: () => walletApi.transactions(),
    retry: false,
  })
  const [amount, setAmount] = useState('100')
  const [notice, setNotice] = useState<string | null>(null)

  const addMoney = useMutation({
    mutationFn: (value: number) => walletApi.addMoney(value, newIdempotencyKey()),
    onSuccess: (wallet) => {
      queryClient.setQueryData(walletQueryKey, wallet)
      void queryClient.invalidateQueries({ queryKey: ['wallet', 'transactions'] })
      setNotice('Money added to your wallet.')
    },
  })

  return (
    <section className="space-y-8">
      <h1 className="font-display text-3xl">Wallet</h1>
      {walletQuery.isLoading ? <Spinner label="Loading wallet…" /> : null}
      {walletQuery.isError ? <Alert>{errorMessage(walletQuery.error)}</Alert> : null}
      {walletQuery.data ? (
        <p className="relative overflow-hidden rounded-[2rem] bg-ink px-6 py-10 text-4xl font-semibold text-cream shadow-lg">
          <span className="relative z-10">{formatInr(walletQuery.data.balance)}</span>
          <span className="relative z-10 mt-2 block text-sm font-normal tracking-[0.18em] text-gold uppercase">
            {walletQuery.data.currency} wallet
          </span>
          <span className="pointer-events-none absolute -right-8 -bottom-10 h-40 w-40 rounded-full bg-saffron/40 blur-2xl" />
        </p>
      ) : null}

      <form
        className="max-w-md space-y-3"
        onSubmit={(event) => {
          event.preventDefault()
          setNotice(null)
          const value = Number(amount)
          addMoney.mutate(value)
        }}
      >
        <h2 className="font-semibold">Add money</h2>
        <div className="flex gap-2">
          {[100, 200, 500].map((chip) => (
            <button
              key={chip}
              type="button"
              className="rounded-full border border-ink/15 px-3 py-1 text-sm"
              onClick={() => setAmount(String(chip))}
            >
              {formatInr(chip)}
            </button>
          ))}
        </div>
        <Field
          label="Amount (₹)"
          type="number"
          min={1}
          max={50000}
          step="1"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
        />
        {addMoney.isError ? <Alert>{errorMessage(addMoney.error)}</Alert> : null}
        {notice ? <Alert tone="success">{notice}</Alert> : null}
        <Button type="submit" pending={addMoney.isPending}>
          Add money
        </Button>
      </form>

      <div>
        <h2 className="font-display text-2xl">Transactions</h2>
        {txQuery.isLoading ? <Spinner label="Loading transactions…" /> : null}
        {txQuery.isError ? <Alert>{errorMessage(txQuery.error)}</Alert> : null}
        {txQuery.data?.content.length === 0 ? <Alert tone="info">No wallet movements yet.</Alert> : null}
        {txQuery.data?.content.length ? (
          <ul className="mt-4 divide-y divide-ink/10 rounded-2xl border border-ink/10 bg-white">
            {txQuery.data.content.map((row) => (
              <li key={row.id} className="flex flex-wrap items-center justify-between gap-2 px-4 py-3 text-sm">
                <div>
                  <p className="font-medium">
                    {row.type} · {row.status}
                  </p>
                  <p className="text-ink/55">{formatDateTime(row.createdAt)}</p>
                </div>
                <p className={row.type === 'DEBIT' ? 'text-red-700' : 'text-emerald-700'}>
                  {row.type === 'DEBIT' ? '−' : '+'}
                  {formatInr(row.amount)}
                </p>
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </section>
  )
}
