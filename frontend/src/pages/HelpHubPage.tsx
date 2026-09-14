import { Link } from 'react-router-dom'


export function HelpHubPage() {
  return (
    <section className="mx-auto max-w-3xl space-y-8">
      <div className="rounded-[2rem] bg-ink px-6 py-10 text-cream md:px-10">
        <p className="text-xs font-semibold tracking-[0.2em] text-gold uppercase">Help &amp; support</p>
        <h1 className="font-display mt-3 text-4xl md:text-5xl">How can we help?</h1>
        <p className="mt-3 max-w-xl text-cream/75">
          Pick one path. Guides and support messages are live. A complaint is a ticket against a paid order.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Choice
          to="/help/guides"
          title="Help guides"
          body="FAQs for ordering, wallet, cancel, and nutrition."
          status="Available now"
          live
        />
        <Choice
          to="/help/chat"
          title="Talk to support"
          body="Send a message. Our team reads it in Kitchen ops → Support."
          status="Available now"
          live
        />
        <Choice
          to="/complaints"
          title="File a complaint"
          body="Report a paid order: quality, missing item, or refund follow-up."
          status="Available now"
          live
        />
      </div>

      <p className="text-sm text-ink/60">
        Support is a conversation. A complaint is a tracked ticket on an order.
      </p>
    </section>
  )
}

function Choice({
  to,
  title,
  body,
  status,
  live = false,
}: {
  to: string
  title: string
  body: string
  status: string
  live?: boolean
}) {
  return (
    <Link
      to={to}
      className="flex flex-col rounded-[1.5rem] border border-ink/10 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-saffron/40 hover:shadow-md"
    >
      <h2 className="font-display text-2xl">{title}</h2>
      <p className="mt-2 flex-1 text-sm text-ink/65">{body}</p>
      <p className={`mt-4 text-xs font-semibold tracking-wide uppercase ${live ? 'text-leaf' : 'text-saffron'}`}>
        {status}
      </p>
    </Link>
  )
}
