import { PUBLIC_APP_URL } from '../../config/env'

export function joinUrl(code: string, baseUrl = PUBLIC_APP_URL) {
  return `${baseUrl.replace(/\/$/, '')}/join/${code}`
}
