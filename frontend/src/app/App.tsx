import { AssessmentsPage } from '../features/assessments/pages/AssessmentsPage'
import { useEffect, useState } from 'react'
import { authApi, type User } from '../features/auth/api/auth'
import { LoginPage, RegisterPage } from '../features/auth/pages/AuthPages'
export function App() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    authApi
      .refresh()
      .then((session) => setUser(session.user))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])
  if (loading) return <main className="authPage">Loading…</main>
  if (!user)
    return location.pathname === '/register' ? (
      <RegisterPage onAuthenticated={setUser} />
    ) : (
      <LoginPage onAuthenticated={setUser} />
    )
  return (
    <>
      <header className="sessionBar">
        <span>{user.displayName}</span>
        <button onClick={() => authApi.logout().finally(() => setUser(null))}>Sign out</button>
      </header>
      <AssessmentsPage />
    </>
  )
}
