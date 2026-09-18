import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { useOrganization } from '../features/workspace/OrganizationContext'
import type { Role } from '../features/organizations/api/organizations'

export function RequireAuth() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <main className="authPage">Loading…</main>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function GuestOnly({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <main className="authPage">Loading…</main>
  if (user) return <Navigate to="/app" replace />
  return children
}

export function RequireOrganizationRole({
  roles,
  children,
}: {
  roles: Role[]
  children: ReactNode
}) {
  const { role, error, organization } = useOrganization()
  if (error) {
    return (
      <p className="alert" role="alert">
        {error}
      </p>
    )
  }
  if (!organization) return <p>Loading workspace…</p>
  if (!role || !roles.includes(role)) {
    return (
      <section className="panel">
        <h1>Forbidden</h1>
        <p>Your role cannot open this page.</p>
      </section>
    )
  }
  return children
}
