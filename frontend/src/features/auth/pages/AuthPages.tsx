import { useState, type FormEvent } from 'react'
import { authApi, type User } from '../api/auth'
import './auth.css'

type Props = { onAuthenticated: (user: User) => void }

export function LoginPage({ onAuthenticated }: Props) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      onAuthenticated((await authApi.login(email, password)).user)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Login failed.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <main className="authPage">
      <form className="authCard" onSubmit={submit}>
        <h1>Sign in</h1>
        <label>
          Email
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {error && <p role="alert">{error}</p>}
        <button disabled={busy} type="submit">
          Sign in
        </button>
        <a href="/register">Create an account</a>
      </form>
    </main>
  )
}

export function RegisterPage({ onAuthenticated }: Props) {
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      onAuthenticated((await authApi.register(email, password, displayName)).user)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Registration failed.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <main className="authPage">
      <form className="authCard" onSubmit={submit}>
        <h1>Create an account</h1>
        <label>
          Name
          <input
            required
            maxLength={200}
            autoComplete="name"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
          />
        </label>
        <label>
          Email
          <input
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            required
            minLength={12}
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <small>Use at least 12 characters.</small>
        {error && <p role="alert">{error}</p>}
        <button disabled={busy} type="submit">
          Create account
        </button>
        <a href="/login">Already have an account?</a>
      </form>
    </main>
  )
}
