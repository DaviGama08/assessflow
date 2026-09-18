import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { WorkspaceNav } from './WorkspaceNav'

describe('WorkspaceNav', () => {
  it('hides management links for participants', () => {
    render(
      <WorkspaceNav
        organizationId="org-1"
        role="PARTICIPANT"
        path="/app/organizations/org-1/members"
      />,
    )
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Members' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Assessments' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Question Bank' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Settings' })).not.toBeInTheDocument()
  })

  it('hides branding settings from instructors', () => {
    render(
      <WorkspaceNav
        organizationId="org-1"
        role="INSTRUCTOR"
        path="/app/organizations/org-1/assessments"
      />,
    )
    expect(screen.getByRole('link', { name: 'Assessments' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Question Bank' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Settings' })).not.toBeInTheDocument()
  })
})
