import { describe, expect, it } from 'vitest'
import { assertCloudProductionEnv } from './env'

describe('assertCloudProductionEnv', () => {
  it('allows Local Live same-origin production builds', () => {
    expect(() =>
      assertCloudProductionEnv({ PROD: true, SAME_ORIGIN: 'true', API: 'http://localhost:8080' }),
    ).not.toThrow()
  })

  it('rejects localhost cloud builds', () => {
    expect(() =>
      assertCloudProductionEnv({
        PROD: true,
        API: 'http://localhost:8080/api/v1',
        APP: 'https://app.example',
        WS: 'wss://api.example/ws',
      }),
    ).toThrow(/localhost/)
  })

  it('requires https and wss for Cloudflare Pages', () => {
    expect(() =>
      assertCloudProductionEnv({
        PROD: true,
        API: 'https://api.example/api/v1',
        APP: 'https://app.example',
        WS: 'wss://api.example/ws',
      }),
    ).not.toThrow()
  })
})
