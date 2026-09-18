import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OrganizationSelectorPage } from './OrganizationPages'

describe('OrganizationSelectorPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('lists organizations the user belongs to', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => [
        {
          id: 'org-1',
          name: 'Solidus Training',
          slug: 'solidus',
          status: 'ACTIVE',
          currentUserRole: 'OWNER',
        },
      ],
    } as Response)
    render(<OrganizationSelectorPage />)
    expect(await screen.findByRole('link', { name: /Solidus Training/ })).toHaveAttribute(
      'href',
      '/app/organizations/org-1',
    )
    expect(screen.getByRole('button', { name: 'Create organization' })).toBeInTheDocument()
  })
})
