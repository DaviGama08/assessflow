import { navigate } from '../../shared/navigation'
import { canManageContent, canManageWorkspace } from '../../shared/permissions'
import type { Role } from '../organizations/api/organizations'

type Props = {
  organizationId: string
  role?: Role
  path: string
}

const items = [
  { suffix: '', label: 'Dashboard', match: 'dashboard' },
  { suffix: '/assessments', label: 'Assessments', content: true },
  { suffix: '/questions', label: 'Question Bank', content: true },
  { suffix: '/members', label: 'Members' },
  { suffix: '/settings', label: 'Settings', workspace: true },
] as const

export function WorkspaceNav({ organizationId, role, path }: Props) {
  const base = `/app/organizations/${organizationId}`
  return (
    <nav aria-label="Workspace">
      {items.map((item) => {
        if ('content' in item && item.content && !canManageContent(role)) return null
        if ('workspace' in item && item.workspace && !canManageWorkspace(role)) return null
        const href = `${base}${item.suffix}`
        const current =
          item.suffix === '' ? path === base || path === `${base}/` : path.startsWith(href)
        return (
          <a
            key={href}
            href={href}
            aria-current={current ? 'page' : undefined}
            onClick={(event) => {
              event.preventDefault()
              navigate(href)
            }}
          >
            {item.label}
          </a>
        )
      })}
    </nav>
  )
}
