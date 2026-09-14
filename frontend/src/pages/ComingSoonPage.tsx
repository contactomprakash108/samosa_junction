import { Link } from 'react-router-dom'


const COPY: Record<string, { title: string; blurb: string }> = {
  help: {
    title: 'Help guides',
    blurb: 'FAQs and how-to articles will live here. Nothing on this screen is invented as an answer.',
  },
  support: {
    title: 'Talk to support',
    blurb: 'Live chat and email for order questions will live here. This is not a complaint ticket.',
  },
  agent: {
    title: 'Ordering agent',
    blurb: 'Samosa AI will take orders in chat. It is warming up.',
  },
}

export function ComingSoonPage({ topic }: { topic: string }) {
  const copy = COPY[topic] ?? {
    title: 'This feature',
    blurb: 'We are still cooking this part of Samosa Junction.',
  }
  const backToHelp = topic === 'help' || topic === 'support'

  return (
    <section className="mx-auto max-w-2xl overflow-hidden rounded-[2rem] border border-ink/10 bg-white shadow-lg">
      <div className="bg-ink px-8 py-10 text-cream">
        <p className="text-xs font-semibold tracking-[0.2em] text-gold uppercase">Coming next</p>
        <h1 className="font-display mt-3 text-4xl">{copy.title}</h1>
        <p className="mt-3 max-w-lg text-cream/75">{copy.blurb}</p>
      </div>
      <div className="space-y-5 px-8 py-8">
        <p className="rounded-2xl bg-cream px-5 py-4 text-lg font-semibold text-ink">Releasing soon</p>
        <div className="flex flex-wrap gap-3 text-sm font-semibold">
          {backToHelp ? (
            <Link to="/help" className="rounded-full bg-saffron px-4 py-2 text-cream hover:bg-saffron-dark">
              Back to Help
            </Link>
          ) : (
            <Link to="/" className="rounded-full bg-saffron px-4 py-2 text-cream hover:bg-saffron-dark">
              Order samosas
            </Link>
          )}
          {backToHelp ? (
            <Link to="/complaints" className="rounded-full border border-ink/15 px-4 py-2 text-ink hover:bg-cream">
              File a complaint instead
            </Link>
          ) : null}
        </div>
      </div>
    </section>
  )
}
