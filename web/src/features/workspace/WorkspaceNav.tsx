import { NavLink } from 'react-router-dom'
import { canManageContent, canManageWorkspace } from '../../shared/permissions'
import type { Role } from '../organizations/api/organizations'

type Props = {
  organizationId: string
  role?: Role
}

const icons: Record<string, string> = {
  Dashboard: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  Assessments: 'M6 3h12v18H6z M9 8h6 M9 12h6 M9 16h4',
  'Local Live':
    'M8 8a6 6 0 0 0 0 8 M16 8a6 6 0 0 1 0 8 M4 4a12 12 0 0 0 0 16 M20 4a12 12 0 0 1 0 16 M12 11v2',
  'Question Bank': 'M4 4h16v16H4z M8 8h8 M8 12h8 M8 16h4',
  Members:
    'M16 21v-3a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v3 M10 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8 M18 5a4 4 0 0 1 0 7 M20 21v-3a4 4 0 0 0-2-3',
  Settings: 'M4 6h16 M4 12h16 M4 18h16 M8 3v6 M16 9v6 M10 15v6',
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
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <path d={icons[item.label]} />
            </svg>
            {item.label}
          </NavLink>
        )
      })}
    </nav>
  )
}
