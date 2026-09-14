import { Outlet, useLocation } from 'react-router-dom'
import { AppFooter } from '@/components/layout/AppFooter'
import { AppHeader } from '@/components/layout/AppHeader'


export function AppLayout() {
  const { pathname } = useLocation()
  const chatMode = pathname.startsWith('/assistant')

  return (
    <div className="flex min-h-dvh flex-col bg-cream text-ink">
      <AppHeader />
      <main
        className={
          chatMode
            ? 'mx-auto w-full max-w-6xl flex-1 px-0 py-0 md:px-4 md:py-4'
            : 'mx-auto w-full max-w-6xl flex-1 px-4 py-6 md:py-10'
        }
      >
        <Outlet />
      </main>
      {chatMode ? null : <AppFooter />}
    </div>
  )
}
