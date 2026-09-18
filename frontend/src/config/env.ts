const devApi = 'http://localhost:8080/api/v1'
const devWs = 'ws://localhost:8080/ws'

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? devApi : '/api/v1')

export const PUBLIC_APP_URL =
  import.meta.env.VITE_PUBLIC_APP_URL ??
  (typeof window !== 'undefined' ? window.location.origin : 'http://localhost:5173')

export const WS_URL =
  import.meta.env.VITE_WS_URL ??
  (import.meta.env.DEV
    ? devWs
    : `${typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws'}://${typeof window !== 'undefined' ? window.location.host : 'localhost:8080'}/ws`)
