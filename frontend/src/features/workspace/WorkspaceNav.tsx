import { NavLink } from 'react-router-dom'
import { canManageContent, canManageWorkspace } from '../../shared/permissions'
import type { Role } from '../organizations/api/organizations'

type Props = {
  organizationId: string
  role?: Role
}

const items = [
  { suffix: '', label: 'Dashboard', end: true },
  { suffix: '/assessments', label: 'Assessments', content: true },
  { suffix: '/local-live', label: 'Local Live', content: true },
  { suffix: '/questions', label: 'Question Bank', content: true },
  { suffix: '/members', label: 'Members' },
  { suffix: '/settings', label: 'Settings', workspace: true },
] as const

export function WorkspaceNav({ organizationId, role }: Props) {
  const base = `/app/organizations/${organizationId}`
  return (
    <nav aria-label="Workspace">
      {items.map((item) => {
        if ('content' in item && item.content && !canManageContent(role)) return null
        if ('workspace' in item && item.workspace && !canManageWorkspace(role)) return null
        return (
          <NavLink key={item.label} to={`${base}${item.suffix}`} end={'end' in item && item.end}>
            {item.label}
          </NavLink>
        )
      })}
    </nav>
  )
}
