import { AssessmentsPage } from '../features/assessments/pages/AssessmentsPage'
import { useEffect, useState } from 'react'
import { authApi, type User } from '../features/auth/api/auth'
import { LoginPage, RegisterPage } from '../features/auth/pages/AuthPages'
import {
  MembersPage,
  OrganizationPage,
  OrganizationSelectorPage,
} from '../features/organizations/pages/OrganizationPages'
export function App() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [path, setPath] = useState(window.location.pathname)
  useEffect(() => {
    authApi
      .refresh()
      .then((session) => {
        setUser(session.user)
        if (['/', '/login', '/register'].includes(window.location.pathname)) {
          window.history.replaceState({}, '', '/app')
          setPath('/app')
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    const updatePath = () => setPath(window.location.pathname)
    window.addEventListener('popstate', updatePath)
    return () => window.removeEventListener('popstate', updatePath)
  }, [])
  function authenticated(nextUser: User) {
    setUser(nextUser)
    window.history.replaceState({}, '', '/app')
    setPath('/app')
  }
  function signOut() {
    void authApi.logout().finally(() => {
      setUser(null)
      window.history.replaceState({}, '', '/login')
      setPath('/login')
    })
  }
  if (loading) return <main className="authPage">Loading…</main>
  if (!user)
    return path === '/register' ? (
      <RegisterPage onAuthenticated={authenticated} />
    ) : (
      <LoginPage onAuthenticated={authenticated} />
    )
  const memberRoute = path.match(/^\/app\/organizations\/([^/]+)\/members\/?$/)
  const organizationRoute = path.match(/^\/app\/organizations\/([^/]+)\/?$/)
  return (
    <>
      <header className="sessionBar">
        <a href="/app">Organizations</a>
        <span>{user.displayName}</span>
        <button onClick={signOut}>Sign out</button>
      </header>
      {memberRoute ? (
        <MembersPage id={memberRoute[1]} user={user} />
      ) : organizationRoute ? (
        <OrganizationPage id={organizationRoute[1]} />
      ) : path === '/app/assessments' ? (
        <AssessmentsPage />
      ) : (
        <OrganizationSelectorPage />
      )}
    </>
  )
}
