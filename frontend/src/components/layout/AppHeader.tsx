import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { BrandLink } from '@/components/brand/BrandMark'
import { useAuth } from '@/hooks/useAuth'
import { useCart } from '@/hooks/useCart'
import { isStaffUser } from '@/utils/roles'


export function AppHeader() {
  const { status, user } = useAuth()
  const { query } = useCart()
  const { pathname } = useLocation()
  const [open, setOpen] = useState(false)
  const cartCount = query.data?.totalQuantity ?? 0

  useEffect(() => {
    setOpen(false)
  }, [pathname])

  return (
    <header className="sticky top-0 z-30 border-b border-ink/10 bg-cream/90 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3">
        <BrandLink />
        <nav className="hidden items-center gap-1 text-sm font-medium lg:flex">
          {links(status, user?.fullName, cartCount, false, isStaffUser(user))}
        </nav>
        <div className="flex items-center gap-2 lg:hidden">
          <NavLink to="/cart" className={cartNavClass}>
            Cart
            {cartCount ? (
              <span className="ml-1.5 rounded-full bg-saffron px-1.5 py-0.5 text-[11px] font-bold text-cream">{cartCount}</span>
            ) : null}
          </NavLink>
          <button
            type="button"
            className="rounded-full border border-ink/15 px-3 py-2 text-sm font-semibold"
            aria-expanded={open}
            aria-label="Open menu"
            onClick={() => setOpen((value) => !value)}
          >
            {open ? 'Close' : 'Menu'}
          </button>
        </div>
      </div>
      {open ? (
        <div className="border-t border-ink/10 bg-cream px-4 py-4 lg:hidden">
          <nav className="flex flex-col gap-2 text-sm font-medium">
            {links(status, user?.fullName, cartCount, true, isStaffUser(user))}
          </nav>
        </div>
      ) : null}
    </header>
  )
}

function links(status: string, fullName: string | undefined, cartCount: number, stacked = false, staff = false) {
  const first = fullName?.split(' ')[0] ?? 'Profile'
  return (
    <>
      <NavLink to="/" className={navClass} end>
        Menu
      </NavLink>
      {staff ? (
        <NavLink to="/staff" className={navClass}>
          Ops
        </NavLink>
      ) : null}
      <NavLink to="/orders" className={navClass}>
        Orders
      </NavLink>
      <NavLink
        to="/assistant"
        className={({ isActive }) =>
          `rounded-full px-3 py-1.5 font-semibold ${
            isActive ? 'bg-saffron text-cream' : 'bg-saffron/15 text-saffron-dark hover:bg-saffron hover:text-cream'
          }`
        }
      >
        Ask Samosa AI
      </NavLink>
      <NavLink to="/wallet" className={navClass}>
        Wallet
      </NavLink>
      {!stacked ? (
        <NavLink to="/cart" className={cartNavClass}>
          Cart
          {cartCount ? (
            <span className="ml-1.5 rounded-full bg-saffron px-1.5 py-0.5 text-[11px] font-bold text-cream">{cartCount}</span>
          ) : null}
        </NavLink>
      ) : (
        <NavLink to="/cart" className={navClass}>
          Cart{cartCount ? ` (${cartCount})` : ''}
        </NavLink>
      )}
      {status === 'authenticated' ? (
        <NavLink to="/profile" className={navClass}>
          {first}
        </NavLink>
      ) : (
        <NavLink to="/login" className={navClass}>
          Log in
        </NavLink>
      )}
    </>
  )
}

function navClass({ isActive }: { isActive: boolean }) {
  return `rounded-full px-3 py-1.5 ${isActive ? 'bg-white text-saffron shadow-sm' : 'text-ink/70 hover:bg-white/80 hover:text-ink'}`
}

function cartNavClass({ isActive }: { isActive: boolean }) {
  return `inline-flex items-center rounded-full px-3 py-1.5 ${
    isActive ? 'bg-ink text-cream' : 'bg-white text-ink shadow-sm hover:bg-cream-dark'
  }`
}
