import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { WorkspaceNav } from './WorkspaceNav'

describe('WorkspaceNav', () => {
  it('hides management links for participants', () => {
    render(
      <MemoryRouter>
        <WorkspaceNav organizationId="org-1" role="PARTICIPANT" />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Members' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Assessments' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Question Bank' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Settings' })).not.toBeInTheDocument()
  })

  it('hides branding settings from instructors', () => {
    render(
      <MemoryRouter>
        <WorkspaceNav organizationId="org-1" role="INSTRUCTOR" />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'Assessments' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Question Bank' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Settings' })).not.toBeInTheDocument()
  })
})
