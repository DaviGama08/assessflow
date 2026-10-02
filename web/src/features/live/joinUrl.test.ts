import { describe, expect, it } from 'vitest'
import { joinUrl } from './joinUrl'

describe('joinUrl', () => {
  it('builds a public join URL from the base and code', () => {
    expect(joinUrl('X7K92P', 'https://assessflow.example/')).toBe(
      'https://assessflow.example/join/X7K92P',
    )
    expect(joinUrl('X7K92P', 'http://192.168.137.1')).toBe('http://192.168.137.1/join/X7K92P')
  })
})
