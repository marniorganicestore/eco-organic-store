import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'
import type { ReactNode } from 'react'

type Props = {
  children: ReactNode
}

function isAdmin(roles: string[]): boolean {
  return roles.includes('ADMIN')
}

export function RequireAuth({ children }: Props) {
  const location = useLocation()
  const { user, bootstrapped } = useAuthStore()

  if (!bootstrapped) {
    return <p className="p-6 text-sm text-emerald-900/80">Restoring session...</p>
  }
  if (!user) {
    const from = `${location.pathname}${location.search}`
    return <Navigate to="/login" replace state={{ from }} />
  }

  return <>{children}</>
}

export function RequireAdmin({ children }: Props) {
  const location = useLocation()
  const { user, bootstrapped } = useAuthStore()

  if (!bootstrapped) {
    return <p className="p-6 text-sm text-emerald-900/80">Restoring session...</p>
  }
  if (!user) {
    const from = `${location.pathname}${location.search}`
    return <Navigate to="/login" replace state={{ from }} />
  }
  if (!isAdmin(user.roles)) return <Navigate to="/shop" replace />
  return <>{children}</>
}
