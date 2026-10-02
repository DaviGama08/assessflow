import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi, type User } from './api/auth'

type AuthValue = {
  user: User | null
  loading: boolean
  signIn: (user: User) => void
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => {
    authApi
      .refresh()
      .then((session) => setUser(session.user))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])
  const value = useMemo<AuthValue>(
    () => ({
      user,
      loading,
      signIn: setUser,
      signOut: async () => {
        await authApi.logout().catch(() => undefined)
        setUser(null)
      },
    }),
    [user, loading],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
