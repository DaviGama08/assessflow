import { API_BASE_URL } from '../../config/env'
import { getAccessToken, refreshAccessToken } from '../../features/auth/api/auth'

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message)
  }
}

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    const send = (token: string | null) =>
      fetch(`${API_BASE_URL}${path}`, {
        ...options,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...options?.headers,
        },
      })
    response = await send(getAccessToken())
    if (response.status === 401 && !path.startsWith('/auth/')) {
      const refreshed = await refreshAccessToken()
      if (refreshed) response = await send(refreshed)
    }
  } catch {
    throw new ApiError(0, 'Could not connect to the API. Check that the backend is running.')
  }
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { detail?: string } | null
    throw new ApiError(response.status, problem?.detail ?? `Request failed (${response.status})`)
  }
  return response.status === 204 ? (undefined as T) : (response.json() as Promise<T>)
}
