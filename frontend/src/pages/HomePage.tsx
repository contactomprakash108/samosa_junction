import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { productApi } from '@/api/productApi'
import { recommendationApi } from '@/api/recommendationApi'
import { ProductCard } from '@/components/product/ProductCard'
import { Alert } from '@/components/ui/Alert'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'
import { categoryLabel } from '@/utils/catalog'
import { errorMessage } from '@/utils/errors'


const PROMPTS = [
  'Order 2 paneer samosas.',
  'Give me something high protein under ₹150.',
  "I'm hungry. What should I try?",
  'Where is my order?',
  'My order arrived damaged.',
]

export function HomePage() {
  const { status } = useAuth()
  const [params, setParams] = useSearchParams()
  const category = params.get('category') ?? 'ALL'
  const [hashReady, setHashReady] = useState(false)

  useEffect(() => {
    setHashReady(true)
    if (window.location.hash === '#menu') {
      document.getElementById('menu')?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [params])

  const categoriesQuery = useQuery({
    queryKey: ['categories'],
    queryFn: productApi.categories,
    retry: false,
  })
  const productsQuery = useQuery({
    queryKey: ['products', { category }],
    queryFn: () =>
      productApi.list({
        category: category === 'ALL' ? undefined : category,
        available: true,
        page: 0,
        size: 50,
        sort: 'name',
      }),
    retry: false,
  })
  const recommendationsQuery = useQuery({
    queryKey: ['recommendations', 4],
    queryFn: () => recommendationApi.list(4),
    enabled: status === 'authenticated',
    retry: false,
  })

  const products = productsQuery.data?.content ?? []
  const showRecommendations =
    status === 'authenticated' &&
    recommendationsQuery.isSuccess &&
    !recommendationsQuery.data.coldStart &&
    recommendationsQuery.data.items.length > 0

  return (
    <div className="space-y-16 md:space-y-24">
      <section className="grid overflow-hidden rounded-[1.75rem] bg-ink text-cream shadow-[0_30px_80px_-40px_rgba(28,21,16,0.8)] lg:grid-cols-[1.05fr_0.95fr]">
        <div className="flex flex-col justify-center px-6 py-12 md:px-12 md:py-16">
          <p className="text-xs font-semibold tracking-[0.24em] text-gold uppercase">Fresh from the tawa</p>
          <h1 className="font-display mt-3 max-w-lg text-4xl leading-[1.08] md:text-6xl">Samosas worth craving.</h1>
          <p className="mt-4 max-w-md text-base text-cream/75 md:text-lg">
            Classic, baked, healthy and protein-packed — made fresh for every kind of craving.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <a
              href="#menu"
              className="inline-flex min-h-11 items-center rounded-full bg-saffron px-5 text-sm font-semibold text-cream hover:bg-saffron-dark"
            >
              Explore Menu
            </a>
            <Link
              to="/assistant"
              className="inline-flex min-h-11 items-center rounded-full border border-cream/25 px-5 text-sm font-semibold text-cream hover:bg-cream/10"
            >
              Ask Samosa AI
            </Link>
          </div>
        </div>
        <div className="relative min-h-64">
          <img
            src="/images/hero-samosas.png"
            alt="Fresh samosas on a terracotta plate"
            className="h-full w-full object-cover"
          />
        </div>
      </section>

      <section id="menu" className="scroll-mt-24 space-y-6">
        <div>
          <h2 className="font-display text-3xl md:text-4xl">What are you craving?</h2>
          <p className="mt-2 text-ink/60">Pick a mood. We’ll show the tray that matches.</p>
        </div>
        {categoriesQuery.isLoading ? <Spinner label="Loading categories…" /> : null}
        {categoriesQuery.isError ? <Alert>{errorMessage(categoriesQuery.error, 'Unable to load categories.')}</Alert> : null}
        {categoriesQuery.data ? (
          <ul className="flex flex-wrap gap-2">
            <li>
              <CategoryChip selected={category === 'ALL'} onClick={() => setParams({})}>
                All
              </CategoryChip>
            </li>
            {categoriesQuery.data.map((item) => (
              <li key={item.code}>
                <CategoryChip
                  selected={category === item.code}
                  onClick={() => setParams({ category: item.code })}
                >
                  {item.name}
                </CategoryChip>
              </li>
            ))}
          </ul>
        ) : null}

        <h3 className="font-display text-2xl">
          {category === 'ALL' ? 'Popular picks' : categoryLabel(category)}
        </h3>
        {productsQuery.isLoading ? <Spinner label="Loading the menu…" /> : null}
        {productsQuery.isError ? <Alert>{errorMessage(productsQuery.error, 'Unable to load the menu.')}</Alert> : null}
        {!productsQuery.isLoading && products.length === 0 ? (
          <Alert tone="info">Nothing in this category right now. Try another craving.</Alert>
        ) : null}
        {products.length > 0 ? (
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        ) : null}
      </section>

      <section className="space-y-6">
        <h2 className="font-display text-3xl md:text-4xl">Why Samosa Junction?</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <WhyCard title="Better ingredients" body="Thoughtfully selected ingredients in every bite." />
          <WhyCard title="Baked options" body="Crispy choices beyond the traditional fry." />
          <WhyCard title="Protein-packed" body="More satisfying options for protein-conscious cravings." />
          <WhyCard title="Know your food" body="Clear nutrition information so you know what you’re ordering." />
        </div>
      </section>

      <section className="overflow-hidden rounded-[1.75rem] border border-ink/10 bg-white">
        <div className="grid lg:grid-cols-2">
          <div className="space-y-5 px-6 py-10 md:px-10">
            <p className="text-xs font-semibold tracking-[0.2em] text-saffron uppercase">Samosa AI</p>
            <h2 className="font-display text-3xl md:text-4xl">Tell us what you’re craving.</h2>
            <p className="text-ink/65">
              Your personal Samosa Assistant can help you discover, choose and order.
            </p>
            <Link
              to="/assistant"
              className="inline-flex min-h-11 items-center rounded-full bg-saffron px-5 text-sm font-semibold text-cream hover:bg-saffron-dark"
            >
              Ask Samosa AI
            </Link>
          </div>
          <div className="bg-cream px-6 py-10 md:px-10">
            <p className="text-sm font-semibold text-ink/70">Try saying</p>
            <ul className="mt-4 space-y-2">
              {PROMPTS.map((prompt) => (
                <li key={prompt}>
                  <Link
                    to={`/assistant?q=${encodeURIComponent(prompt)}`}
                    className="block rounded-2xl border border-ink/10 bg-white px-4 py-3 text-sm text-ink/80 hover:border-saffron/40"
                  >
                    “{prompt}”
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {showRecommendations ? (
        <section className="space-y-4">
          <div>
            <h2 className="font-display text-3xl">Picked for you</h2>
            <p className="mt-1 text-sm text-ink/60">Based on what you’ve ordered before.</p>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
            {recommendationsQuery.data.items.map((item) => (
              <ProductCard key={item.product.id} product={item.product} />
            ))}
          </div>
        </section>
      ) : hashReady && status === 'authenticated' && recommendationsQuery.isSuccess ? (
        <section className="rounded-[1.75rem] border border-ink/10 bg-white px-6 py-8">
          <h2 className="font-display text-3xl">Picked for you</h2>
          <p className="mt-2 max-w-xl text-ink/65">
            After a few paid orders, this tray will fill with samosas that match your taste. Until then, explore the menu.
          </p>
        </section>
      ) : null}

      <section className="grid gap-8 overflow-hidden rounded-[1.75rem] bg-cream-dark/40 md:grid-cols-2">
        <img src="/images/products/classic-samosa.png" alt="" className="h-full min-h-56 w-full object-cover" />
        <div className="flex flex-col justify-center px-6 py-10 md:pr-12">
          <p className="text-xs font-semibold tracking-[0.2em] text-saffron uppercase">Our story</p>
          <h2 className="font-display mt-2 text-3xl">Why should you have to choose?</h2>
          <p className="mt-3 text-ink/70">
            India loves samosas. Then many of us started wanting better options — lighter methods, more protein, nutrition
            we can read. Samosa Junction was born to reimagine the snack we already love, without hiding what’s inside.
          </p>
          <Link to="/story" className="mt-5 text-sm font-semibold text-saffron">
            Read our story
          </Link>
        </div>
      </section>

      <section className="rounded-[1.75rem] bg-ink px-6 py-12 text-center text-cream md:px-12">
        <h2 className="font-display text-3xl md:text-5xl">Ready for a better samosa?</h2>
        <p className="mx-auto mt-3 max-w-lg text-cream/70">
          The samosa we love. Reimagined for the way we eat today.
        </p>
        <a
          href="#menu"
          className="mt-8 inline-flex min-h-11 items-center rounded-full bg-saffron px-6 text-sm font-semibold text-cream hover:bg-saffron-dark"
        >
          Explore Menu
        </a>
      </section>
    </div>
  )
}

function WhyCard({ title, body }: { title: string; body: string }) {
  return (
    <article className="rounded-2xl border border-ink/10 bg-white px-5 py-6">
      <h3 className="text-xs font-semibold tracking-[0.16em] text-saffron uppercase">{title}</h3>
      <p className="mt-3 text-sm text-ink/70">{body}</p>
    </article>
  )
}

function CategoryChip({
  selected,
  onClick,
  children,
}: {
  selected: boolean
  onClick: () => void
  children: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`min-h-10 rounded-full px-4 text-sm font-medium transition ${
        selected ? 'bg-saffron text-cream shadow-sm' : 'border border-ink/10 bg-white text-ink hover:border-saffron/40'
      }`}
    >
      {children}
    </button>
  )
}
