import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { RequireOrganizationRole } from './guards'

vi.mock('../features/workspace/OrganizationContext', () => ({
  useOrganization: () => ({
    organizationId: 'org-1',
    organization: {
      id: 'org-1',
      name: 'Org',
      slug: 'org',
      status: 'ACTIVE',
      currentUserRole: 'INSTRUCTOR',
      createdAt: '',
      updatedAt: '',
    },
    branding: null,
    role: 'INSTRUCTOR',
    error: '',
  }),
}))

describe('RequireOrganizationRole', () => {
  it('shows forbidden for instructors on settings', () => {
    render(
      <MemoryRouter initialEntries={['/settings']}>
        <Routes>
          <Route
            path="/settings"
            element={
              <RequireOrganizationRole roles={['OWNER', 'ADMIN']}>
                <form>
                  <label>
                    Name
                    <input />
                  </label>
                </form>
              </RequireOrganizationRole>
            }
          />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('Forbidden')).toBeInTheDocument()
    expect(screen.queryByLabelText('Name')).not.toBeInTheDocument()
  })
})
