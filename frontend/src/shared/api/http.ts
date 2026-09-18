import { API_BASE_URL } from '../../config/env'

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options?.headers },
    })
  } catch {
    throw new ApiError(0, 'Could not connect to the API. Check that the backend is running.')
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new ApiError(response.status, problem?.detail ?? `Request failed (${response.status})`)
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
