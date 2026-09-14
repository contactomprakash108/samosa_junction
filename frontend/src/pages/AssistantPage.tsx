import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { assistantApi } from '@/api/assistantApi'
import { Button } from '@/components/ui/Button'
import { useAuth } from '@/hooks/useAuth'
import { useCart } from '@/hooks/useCart'
import type { AssistantHistoryMessage } from '@/types/assistant'
import { errorMessage } from '@/utils/errors'
import { formatInr } from '@/utils/money'


const PROMPTS = [
  'Order 2 paneer samosas and pay from my wallet.',
  'What is the best samosa for a paneer lover?',
  'Give me something high protein under ₹150.',
  'Where is my order?',
  'My order arrived damaged.',
]

const WELCOME =
  'Ask for a craving, add to cart, or place an order. I ship to the default address on your Profile and pay from your wallet — after checking stock.'

type Bubble = {
  role: 'you' | 'ai'
  text: string
  products?: { id: string; name: string; price: number }[]
}

function toHistory(bubbles: Bubble[]): AssistantHistoryMessage[] {
  return bubbles
    .filter((bubble) => bubble.role === 'you' || bubble.role === 'ai')
    .slice(-16)
    .map((bubble) => ({
      role: bubble.role === 'you' ? 'user' : 'assistant',
      content: bubble.text,
    }))
}

export function AssistantPage() {
  const [params] = useSearchParams()
  const { status } = useAuth()
  const queryClient = useQueryClient()
  const { resetAfterCheckout, invalidate } = useCart()
  const [draft, setDraft] = useState(params.get('q') ?? '')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [bubbles, setBubbles] = useState<Bubble[]>([
    { role: 'ai', text: WELCOME },
  ])
  const scrollerRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)
  const loggedIn = status === 'authenticated'
  const canSend = useMemo(() => draft.trim().length > 0 && !pending, [draft, pending])
  const started = bubbles.some((bubble) => bubble.role === 'you')

  useEffect(() => {
    const node = scrollerRef.current
    if (!node) {
      return
    }
    node.scrollTo({ top: node.scrollHeight, behavior: 'smooth' })
  }, [bubbles, pending])

  useEffect(() => {
    const starter = params.get('q')
    if (starter && loggedIn) {
      void send(starter)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- one-shot deep link
  }, [loggedIn])

  function newChat() {
    setBubbles([{ role: 'ai', text: WELCOME }])
    setDraft('')
    setError(null)
    setPending(false)
    inputRef.current?.focus()
  }

  async function send(text: string) {
    const message = text.trim()
    if (!message || !loggedIn || pending) {
      return
    }
    const history = toHistory(bubbles)
    setPending(true)
    setError(null)
    setBubbles((current) => [...current, { role: 'you', text: message }])
    setDraft('')
    try {
      const response = await assistantApi.chat(message, history)
      const placed = /placed|cart is (empty|cleared)|paid from wallet/i.test(response.reply)
      if (placed) {
        await resetAfterCheckout()
      } else {
        await invalidate()
      }
      await queryClient.invalidateQueries({ queryKey: ['wallet'] })
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
      await queryClient.invalidateQueries({ queryKey: ['users', 'me'] })
      setBubbles((current) => [
        ...current,
        {
          role: 'ai',
          text: response.reply,
          products: response.products.map((product) => ({
            id: product.id,
            name: product.name,
            price: product.price,
          })),
        },
      ])
    } catch (cause) {
      setError(errorMessage(cause, 'Samosa AI could not answer. Try again.'))
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="flex h-[calc(100dvh-3.75rem)] flex-col overflow-hidden bg-[radial-gradient(circle_at_top_left,_#f7d9b8_0%,_transparent_42%),radial-gradient(circle_at_bottom_right,_#e8c9a0_0%,_transparent_45%),#f4ead9] md:h-[calc(100dvh-5rem)] md:rounded-[1.75rem] md:border md:border-ink/10 md:shadow-sm">
      <header className="flex shrink-0 items-center justify-between gap-3 border-b border-ink/10 bg-ink px-4 py-3 text-cream md:rounded-t-[1.75rem] md:px-6">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold tracking-[0.22em] text-gold uppercase">Samosa AI</p>
          <h1 className="font-display truncate text-xl md:text-2xl">Cravings, cart, checkout</h1>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            type="button"
            onClick={newChat}
            className="rounded-full border border-cream/25 px-3 py-1.5 text-sm font-semibold text-cream hover:bg-cream/10"
          >
            New chat
          </button>
          <Link to="/cart" className="hidden rounded-full bg-saffron px-3 py-1.5 text-sm font-semibold text-cream sm:inline">
            Cart
          </Link>
        </div>
      </header>

      <div ref={scrollerRef} className="min-h-0 flex-1 space-y-3 overflow-y-auto px-3 py-4 md:px-6">
        {!loggedIn ? (
          <p className="rounded-2xl bg-white/80 px-4 py-3 text-sm text-ink/75 shadow-sm">
            <Link to="/login" className="font-semibold text-saffron">
              Log in
            </Link>{' '}
            so I can use your cart, wallet, and last delivery address.
          </p>
        ) : null}

        {!started ? (
          <div className="mx-auto flex max-w-lg flex-col items-center gap-4 px-2 py-8 text-center">
            <p className="font-display text-3xl text-ink md:text-4xl">What are you craving?</p>
            <p className="max-w-md text-sm text-ink/65">{WELCOME}</p>
            <div className="flex w-full flex-wrap justify-center gap-2">
              {PROMPTS.map((prompt) => (
                <button
                  key={prompt}
                  type="button"
                  className="rounded-2xl border border-ink/10 bg-white/90 px-3 py-2 text-left text-sm text-ink/80 shadow-sm transition hover:border-saffron/40"
                  onClick={() => {
                    if (loggedIn) {
                      void send(prompt)
                    } else {
                      setDraft(prompt)
                    }
                  }}
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <ul className="mx-auto flex max-w-2xl flex-col gap-3">
            {bubbles.map((bubble, index) => (
              <li
                key={`${bubble.role}-${index}`}
                className={`max-w-[92%] rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm ${
                  bubble.role === 'you'
                    ? 'ml-auto bg-saffron text-cream'
                    : 'mr-auto border border-ink/8 bg-white text-ink/85'
                }`}
              >
                <p className="whitespace-pre-wrap">{bubble.text}</p>
                {bubble.products?.length ? (
                  <ul className="mt-2 space-y-1 border-t border-ink/10 pt-2">
                    {bubble.products.map((product) => (
                      <li key={product.id}>
                        <Link to={`/products/${product.id}`} className="font-semibold text-saffron">
                          {product.name}
                        </Link>{' '}
                        <span className={bubble.role === 'you' ? 'text-cream/80' : 'text-ink/55'}>
                          {formatInr(product.price)}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </li>
            ))}
            {pending ? (
              <li className="mr-auto rounded-2xl border border-ink/8 bg-white px-4 py-3 text-sm text-ink/55 shadow-sm">
                Thinking with the live menu…
              </li>
            ) : null}
          </ul>
        )}
      </div>

      <form
        className="shrink-0 border-t border-ink/10 bg-white/95 px-3 py-3 backdrop-blur md:rounded-b-[1.75rem] md:px-6"
        onSubmit={(event) => {
          event.preventDefault()
          void send(draft)
        }}
      >
        {error ? <p className="mb-2 text-sm text-red-700">{error}</p> : null}
        <div className="mx-auto flex max-w-2xl items-end gap-2">
          <label className="block min-w-0 flex-1">
            <span className="sr-only">Message</span>
            <textarea
              ref={inputRef}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault()
                  void send(draft)
                }
              }}
              rows={1}
              className="max-h-32 min-h-11 w-full resize-none rounded-2xl border border-ink/15 bg-cream/60 px-3.5 py-2.5 text-sm outline-none focus:border-saffron focus:ring-4 focus:ring-saffron/20"
              placeholder={loggedIn ? 'Ask anything about the menu…' : 'Log in to chat'}
              disabled={!loggedIn || pending}
            />
          </label>
          <Button type="submit" className="shrink-0" pending={pending} disabled={!loggedIn || !canSend}>
            Send
          </Button>
        </div>
        <p className="mx-auto mt-2 max-w-2xl text-[11px] text-ink/45">
          Orders pay from wallet to your last delivery address. Enter sends · Shift+Enter for a new line.
        </p>
      </form>
    </section>
  )
}
