import { API_BASE_URL } from '../../../config/env'
import { request } from '../../../shared/api/http'

export type LocalLiveStatus = {
  mode: string
  internetRequired: boolean
  selectedHost: string
  port: number
  lanAddresses: string[]
  joinBaseUrl: string
  hotspotStatus: string
  captivePortal: string
  database: string
  websocket: string
}

export type HotspotInfo = {
  available: boolean
  ssid: string | null
  wifiQr: string | null
  message: string
}

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
  joinUrl: (code: string) => request<{ url: string }>(`/local-live/join-url/${code}`),
  hotspot: () => request<HotspotInfo>('/local-live/hotspot'),
  startHotspot: () => request<HotspotInfo>('/local-live/hotspot/start', { method: 'POST' }),
  stopHotspot: () => request<HotspotInfo>('/local-live/hotspot/stop', { method: 'POST' }),
  importPackage: (body: unknown) =>
    request<{ organizationId: string; assessmentId: string; title: string }>(
      '/local-live/packages',
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
    ),
}
