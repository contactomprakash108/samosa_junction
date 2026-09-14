import { Link } from 'react-router-dom'


export function NotFoundPage() {
  return (
    <section className="py-16 text-center">
      <h1 className="font-display text-3xl">Page not found</h1>
      <p className="mt-2 text-ink/65">That page wandered off the menu.</p>
      <Link to="/" className="mt-6 inline-block font-semibold text-saffron">
        Back to the menu
      </Link>
    </section>
  )
}
