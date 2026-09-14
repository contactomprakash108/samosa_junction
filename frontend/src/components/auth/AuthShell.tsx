import type { ReactNode } from 'react'
import { BrandMark } from '@/components/brand/BrandMark'


export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle: string
  children: ReactNode
}) {
  return (
    <div className="grid overflow-hidden rounded-[2rem] border border-ink/10 bg-white shadow-[0_24px_80px_-32px_rgba(31,25,21,0.45)] lg:grid-cols-[1.05fr_1fr]">
      <aside className="relative hidden overflow-hidden bg-ink px-10 py-12 text-cream lg:flex lg:flex-col lg:justify-between">
        <div className="pointer-events-none absolute inset-0 opacity-40" aria-hidden>
          <div className="absolute -top-16 -left-10 h-56 w-56 rounded-full bg-saffron/50 blur-3xl" />
          <div className="absolute right-0 bottom-0 h-64 w-64 rounded-full bg-gold/30 blur-3xl" />
        </div>
        <div className="relative space-y-6">
          <BrandMark className="h-12 w-12" />
          <p className="text-xs font-semibold tracking-[0.22em] text-gold uppercase">Samosa Junction</p>
          <h2 className="font-display max-w-sm text-4xl leading-tight">The samosa we love. Reimagined for today.</h2>
          <p className="max-w-sm text-sm text-cream/70">
            Sign in to keep your tray, wallet, and orders in one warm place.
          </p>
        </div>
        <p className="relative text-sm text-cream/55">Veg-first snacks, folded fresh.</p>
      </aside>
      <div className="px-6 py-10 sm:px-10">
        <h1 className="font-display text-3xl text-ink">{title}</h1>
        <p className="mt-2 text-ink/65">{subtitle}</p>
        <div className="mt-8">{children}</div>
      </div>
    </div>
  )
}
