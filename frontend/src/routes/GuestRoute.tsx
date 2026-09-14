import { Navigate, Outlet } from 'react-router-dom'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'


export function GuestRoute() {
  const { status } = useAuth()

  if (status === 'loading') {
    return <Spinner label="Checking your session…" />
  }
  if (status === 'authenticated') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
