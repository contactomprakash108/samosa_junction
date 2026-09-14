import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'


export function AppFooter() {
  return (
    <footer className="mt-auto bg-ink text-cream">
      <div className="mx-auto grid max-w-6xl gap-10 px-4 py-14 sm:grid-cols-2 lg:grid-cols-6">
        <div className="space-y-4 sm:col-span-2">
          <div className="flex items-center gap-2">
            <BrandMark className="h-9 w-9" />
            <p className="font-display text-2xl">Samosa Junction</p>
          </div>
          <p className="max-w-sm font-display text-xl leading-snug text-cream/90">
            The samosa we love.
            <br />
            Reimagined for the way we eat today.
          </p>
        </div>
        <FooterCol title="Explore">
          <Link to="/#menu">Menu</Link>
          <Link to="/?category=ALL#menu">Popular</Link>
          <Link to="/?category=HEALTHY#menu">Healthy</Link>
          <Link to="/?category=PROTEIN#menu">High Protein</Link>
          <Link to="/?category=BAKED#menu">Baked</Link>
        </FooterCol>
        <FooterCol title="Order">
          <Link to="/cart">Cart</Link>
          <Link to="/orders">Orders</Link>
          <Link to="/wallet">Wallet</Link>
          <Link to="/orders">Track order</Link>
        </FooterCol>
        <FooterCol title="Help">
          <Link to="/support">Support</Link>
          <Link to="/complaints">Complaints</Link>
          <Link to="/help/guides">FAQs</Link>
          <Link to="/contact">Contact us</Link>
        </FooterCol>
        <FooterCol title="Company">
          <Link to="/about">About us</Link>
          <Link to="/story">Our story</Link>
          <Link to="/careers">Careers</Link>
          <Link to="/privacy">Privacy</Link>
          <Link to="/terms">Terms</Link>
        </FooterCol>
      </div>
      <div className="border-t border-cream/10">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-5 text-sm text-cream/55">
          <p>© 2026 Samosa Junction</p>
          <p>Made with ❤️ for samosa lovers.</p>
        </div>
      </div>
    </footer>
  )
}

function FooterCol({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <p className="text-xs font-semibold tracking-[0.18em] text-gold uppercase">{title}</p>
      <div className="mt-3 flex flex-col gap-2 text-sm text-cream/75 [&_a:hover]:text-cream">{children}</div>
    </div>
  )
}
