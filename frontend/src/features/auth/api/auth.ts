import { API_BASE_URL } from '../../../config/env'

export type User = { id: string; email: string; displayName: string; status: string }
type Session = { accessToken: string; user: User }
let accessToken: string | null = null
export const getAccessToken = () => accessToken

async function post(path: string, body?: object): Promise<Session> {
  const response = await fetch(`${API_BASE_URL}/auth/${path}`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { detail?: string } | null
    throw new Error(problem?.detail ?? 'Authentication failed.')
  }
  const session = (await response.json()) as Session
  accessToken = session.accessToken
  return session
}
export const authApi = {
  register: (email: string, password: string, displayName: string) =>
    post('register', { email, password, displayName }),
  login: (email: string, password: string) => post('login', { email, password }),
  refresh: () => post('refresh'),
  async logout() {
    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {},
      })
    } finally {
      accessToken = null
    }
  },
}
export async function refreshAccessToken(): Promise<string | null> {
  try {
    return (await authApi.refresh()).accessToken
  } catch {
    accessToken = null
    return null
  }
}
