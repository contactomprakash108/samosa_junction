import { Link } from 'react-router-dom'


const FAQS = [
  {
    q: 'How do I order?',
    a: 'Browse the menu, add samosas to your cart, then checkout with a delivery address. You can pay from your wallet now or place an unpaid order and pay after.',
  },
  {
    q: 'What can I pay with?',
    a: 'Wallet (add money on the Wallet page) or a test card at checkout. The test card does not charge a real bank.',
  },
  {
    q: 'When can I cancel?',
    a: 'You can cancel while the order is unpaid, confirmed, or still being prepared. Once it is ready or out for delivery, cancel is closed.',
  },
  {
    q: 'Where is nutrition from?',
    a: 'Calories, protein, and spice on each card come from the product listing. We do not invent extra health claims on this page.',
  },
  {
    q: 'What is a complaint vs support?',
    a: 'Support is a message to the kitchen about an account or delivery question. A complaint is a ticket tied to a paid order — quality, missing item, or similar.',
  },
  {
    q: 'How does password reset work?',
    a: 'Use Forgot password on the login screen. We send a reset link to the email on the account. Locally, a continue button may appear so you can finish without a mailbox.',
  },
]

export function HelpFaqPage() {
  return (
    <section className="mx-auto max-w-2xl space-y-6">
      <div>
        <Link to="/help" className="text-sm font-semibold text-saffron">
          Help
        </Link>
        <h1 className="font-display mt-2 text-4xl">Help guides</h1>
        <p className="mt-2 text-ink/65">Straight answers for ordering at Samosa Junction.</p>
      </div>
      <ul className="space-y-4">
        {FAQS.map((item) => (
          <li key={item.q} className="rounded-2xl border border-ink/10 bg-white px-5 py-4">
            <h2 className="font-semibold text-ink">{item.q}</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink/70">{item.a}</p>
          </li>
        ))}
      </ul>
    </section>
  )
}
