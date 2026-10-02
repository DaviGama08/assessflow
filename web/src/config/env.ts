const devApi = 'http://localhost:8080/api/v1'
const devWs = 'ws://localhost:8080/ws'

export function assertCloudProductionEnv(env: {
  PROD: boolean
  SAME_ORIGIN?: string
  API?: string
  APP?: string
  WS?: string
}) {
  if (!env.PROD || env.SAME_ORIGIN === 'true') return
  assertCloudUrl('VITE_API_BASE_URL', env.API, 'http')
  assertCloudUrl('VITE_PUBLIC_APP_URL', env.APP, 'http')
  assertCloudUrl('VITE_WS_URL', env.WS, 'ws')
}

function assertCloudUrl(name: string, value: string | undefined, kind: 'http' | 'ws') {
  if (!value || value.trim() === '') {
    throw new Error(
      `${name} must be set for a cloud production build (or set VITE_SAME_ORIGIN=true for Local Live).`,
    )
  }
  if (/localhost|127\.0\.0\.1/i.test(value)) {
    throw new Error(`${name} must not point at localhost in a cloud production build.`)
  }
  const allowed = kind === 'ws' ? /^wss:\/\//i : /^https:\/\//i
  if (!allowed.test(value) && kind === 'http' && value.startsWith('/')) {
    throw new Error(`${name} must be an absolute https URL for Cloudflare Pages.`)
  }
  if (kind === 'ws' && !/^wss:\/\//i.test(value)) {
    throw new Error(`${name} must be a wss:// URL for production.`)
  }
  if (kind === 'http' && !value.startsWith('/') && !/^https:\/\//i.test(value)) {
    throw new Error(`${name} must be an https:// URL for production.`)
  }
}

assertCloudProductionEnv({
  PROD: import.meta.env.PROD,
  SAME_ORIGIN: import.meta.env.VITE_SAME_ORIGIN,
  API: import.meta.env.VITE_API_BASE_URL,
  APP: import.meta.env.VITE_PUBLIC_APP_URL,
  WS: import.meta.env.VITE_WS_URL,
})

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
