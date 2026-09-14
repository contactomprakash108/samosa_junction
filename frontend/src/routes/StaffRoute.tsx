import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Spinner } from '@/components/ui/Spinner'
import { useAuth } from '@/hooks/useAuth'
import { isStaffUser } from '@/utils/roles'


export function StaffRoute() {
  const { status, user } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <Spinner label="Checking staff access…" />
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (!isStaffUser(user)) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
