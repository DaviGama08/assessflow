import { useEffect, useState, type ReactNode } from 'react'
import type { User } from '../auth/api/auth'
import {
  organizationsApi,
  type Branding,
  type Organization,
} from '../organizations/api/organizations'
import { navigate } from '../../shared/navigation'
import { WorkspaceNav } from './WorkspaceNav'
import './workspace.css'

type Props = {
  organizationId: string
  user: User
  path: string
  onSignOut: () => void
  children: ReactNode
}

export function WorkspaceLayout({ organizationId, user, path, onSignOut, children }: Props) {
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [branding, setBranding] = useState<Branding | null>(null)
  const [error, setError] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const primary = branding?.primaryColor || '#5369e8'
  const secondary = branding?.secondaryColor || '#101e32'
  const displayName = branding?.displayName || organization?.name || 'Workspace'

  useEffect(() => {
    setError('')
    Promise.all([organizationsApi.get(organizationId), organizationsApi.branding(organizationId)])
      .then(([nextOrganization, nextBranding]) => {
        setOrganization(nextOrganization)
        setBranding(nextBranding)
      })
      .catch((cause) =>
        setError(cause instanceof Error ? cause.message : 'Could not load workspace.'),
      )
  }, [organizationId])

  useEffect(() => {
    document.documentElement.style.setProperty('--af-primary', primary)
    document.documentElement.style.setProperty('--af-secondary', secondary)
  }, [primary, secondary])

  useEffect(() => {
    setMenuOpen(false)
  }, [path])

  return (
    <div className="workspace">
      <a className="workspaceSkip" href="#workspace-main">
        Skip to content
      </a>
      <aside id="workspace-nav" className={`workspaceNav${menuOpen ? ' open' : ''}`}>
        <a
          className="workspaceBrand"
          href="/app"
          onClick={(event) => {
            event.preventDefault()
            navigate('/app')
          }}
        >
          {branding?.logoUrl ? (
            <img src={branding.logoUrl} alt="" />
          ) : (
            <span className="workspaceMark">AF</span>
          )}
          <span>
            <strong>AssessFlow</strong>
            <small>powered workspace: {displayName}</small>
          </span>
        </a>
        <WorkspaceNav
          organizationId={organizationId}
          role={organization?.currentUserRole}
          path={path}
        />
      </aside>
      <div className="workspaceMain">
        <header className="workspaceHeader">
          <div>
            <button
              className="workspaceMenuToggle buttonSecondary"
              aria-expanded={menuOpen}
              aria-controls="workspace-nav"
              onClick={() => setMenuOpen((open) => !open)}
            >
              Menu
            </button>
            <div className="powered">AssessFlow · {organization?.name ?? 'Organization'}</div>
          </div>
          <div className="workspaceUser">
            <span>{user.displayName}</span>
            <button className="buttonSecondary" onClick={onSignOut}>
              Sign out
            </button>
          </div>
        </header>
        <main id="workspace-main" className="workspaceContent">
          {error && (
            <p className="alert" role="alert">
              {error}
            </p>
          )}
          {children}
        </main>
      </div>
    </div>
  )
}
