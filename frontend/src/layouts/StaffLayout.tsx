import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'


const LINKS = [
  { to: '/staff', label: 'Dashboard', end: true },
  { to: '/staff/kitchen', label: 'Kitchen' },
  { to: '/staff/orders', label: 'Orders' },
  { to: '/staff/inventory', label: 'Inventory' },
  { to: '/staff/products', label: 'Products' },
  { to: '/staff/complaints', label: 'Complaints' },
  { to: '/staff/support', label: 'Support' },
]

export function StaffLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-dvh bg-zinc-950 text-zinc-100">
      <header className="border-b border-zinc-800 bg-zinc-900">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <p className="text-sm font-semibold tracking-wide text-amber-400 uppercase">Kitchen ops</p>
          <nav className="flex flex-wrap gap-1 text-sm">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 ${isActive ? 'bg-zinc-800 text-white' : 'text-zinc-400 hover:bg-zinc-800 hover:text-white'}`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-3 text-sm text-zinc-400">
            <span>{user?.fullName}</span>
            <NavLink to="/" className="hover:text-white">
              Customer site
            </NavLink>
            <button type="button" className="hover:text-white" onClick={logout}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
