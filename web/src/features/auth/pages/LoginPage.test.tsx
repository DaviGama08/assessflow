import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { LoginPage } from './AuthPages'

describe('LoginPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('signs in and reports errors', async () => {
    const onAuthenticated = vi.fn()
    const user = userEvent.setup()
    render(<LoginPage onAuthenticated={onAuthenticated} />)
    await user.type(screen.getByLabelText('Email'), 'member@example.com')
    await user.type(screen.getByLabelText('Password'), 'secure-password-123')
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        accessToken: 'token',
        user: { id: '1', email: 'member@example.com', displayName: 'Member', status: 'ACTIVE' },
      }),
    } as Response)
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(onAuthenticated).toHaveBeenCalledWith({
      id: '1',
      email: 'member@example.com',
      displayName: 'Member',
      status: 'ACTIVE',
    })
  })
})
