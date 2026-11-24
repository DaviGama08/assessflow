import { API_BASE_URL } from '../../../config/env'

export type LocalLiveStatus = { selectedHost: string; joinBaseUrl: string }

export const localLiveApi = {
  async status(): Promise<LocalLiveStatus | null> {
    try {
      const response = await fetch(`${API_BASE_URL}/local-live/status`)
      if (response.status === 404) return null
      if (!response.ok) return null
      return (await response.json()) as LocalLiveStatus
    } catch {
      return null
    }
  },
}
