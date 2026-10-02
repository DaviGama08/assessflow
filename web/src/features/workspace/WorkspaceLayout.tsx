import { useEffect, useState, type ReactNode } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import type { User } from '../auth/api/auth'
import { useOrganization } from './OrganizationContext'
import { WorkspaceNav } from './WorkspaceNav'
import './workspace.css'

type Props = {
  user: User
  onSignOut: () => void
  children?: ReactNode
}

export function WorkspaceLayout({ user, onSignOut, children }: Props) {
  const { organizationId, organization, branding, role, error } = useOrganization()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const primary = branding?.primaryColor || '#5369e8'
  const secondary = branding?.secondaryColor || '#101e32'
  const displayName = branding?.displayName || organization?.name || 'Workspace'

  useEffect(() => {
    document.documentElement.style.setProperty('--af-primary', primary)
    document.documentElement.style.setProperty('--af-secondary', secondary)
  }, [primary, secondary])

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  return (
    <div className="workspace">
      <a className="workspaceSkip" href="#workspace-main">
        Skip to content
      </a>
      <aside id="workspace-nav" className={`workspaceNav${menuOpen ? ' open' : ''}`}>
        <Link className="workspaceBrand" to="/app">
          {branding?.logoUrl ? (
            <img src={branding.logoUrl} alt="" />
          ) : (
            <span className="workspaceMark">AF</span>
          )}
          <span>
            <strong>AssessFlow</strong>
            <small>powered workspace: {displayName}</small>
          </span>
        </Link>
        <WorkspaceNav organizationId={organizationId} role={role} />
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
          {children ?? <Outlet />}
        </main>
      </div>
    </div>
  )
}
