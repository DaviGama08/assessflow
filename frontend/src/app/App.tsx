import { useEffect, useState } from 'react'
import { AssessmentsPage } from '../features/assessments/pages/AssessmentsPage'
import { AssessmentBuilderPage } from '../features/assessments/pages/AssessmentBuilderPage'
import { authApi, type User } from '../features/auth/api/auth'
import { LoginPage, RegisterPage } from '../features/auth/pages/AuthPages'
import {
  MembersPage,
  OrganizationSelectorPage,
} from '../features/organizations/pages/OrganizationPages'
import { QuestionBankPage } from '../features/questions/pages/QuestionBankPage'
import { QuestionEditorPage } from '../features/questions/pages/QuestionEditorPage'
import { DashboardPage } from '../features/workspace/DashboardPage'
import { SettingsPage } from '../features/workspace/SettingsPage'
import { WorkspaceLayout } from '../features/workspace/WorkspaceLayout'

export function App() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [path, setPath] = useState(window.location.pathname)
  useEffect(() => {
    authApi
      .refresh()
      .then((session) => {
        setUser(session.user)
        if (['/', '/login', '/register'].includes(window.location.pathname)) {
          window.history.replaceState({}, '', '/app')
          setPath('/app')
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])
  useEffect(() => {
    const updatePath = () => setPath(window.location.pathname)
    window.addEventListener('popstate', updatePath)
    return () => window.removeEventListener('popstate', updatePath)
  }, [])
  function authenticated(nextUser: User) {
    setUser(nextUser)
    window.history.replaceState({}, '', '/app')
    setPath('/app')
  }
  function signOut() {
    void authApi.logout().finally(() => {
      setUser(null)
      window.history.replaceState({}, '', '/login')
      setPath('/login')
    })
  }
  if (loading) return <main className="authPage">Loading…</main>
  if (!user)
    return path === '/register' ? (
      <RegisterPage onAuthenticated={authenticated} />
    ) : (
      <LoginPage onAuthenticated={authenticated} />
    )

  const workspace = path.match(
    /^\/app\/organizations\/([^/]+)(?:\/(assessments|questions|members|settings)(?:\/([^/]+))?)?\/?$/,
  )
  if (workspace) {
    const organizationId = workspace[1]
    const section = workspace[2]
    const resourceId = workspace[3]
    let content
    if (section === 'assessments' && resourceId) {
      content = <AssessmentBuilderPage organizationId={organizationId} assessmentId={resourceId} />
    } else if (section === 'assessments') {
      content = <AssessmentsPage organizationId={organizationId} />
    } else if (section === 'questions' && resourceId === 'new') {
      content = <QuestionEditorPage organizationId={organizationId} />
    } else if (section === 'questions' && resourceId) {
      content = <QuestionEditorPage organizationId={organizationId} questionId={resourceId} />
    } else if (section === 'questions') {
      content = <QuestionBankPage organizationId={organizationId} />
    } else if (section === 'members') {
      content = <MembersPage id={organizationId} user={user} />
    } else if (section === 'settings') {
      content = <SettingsPage organizationId={organizationId} />
    } else {
      content = <DashboardPage organizationId={organizationId} />
    }
    return (
      <WorkspaceLayout organizationId={organizationId} user={user} path={path} onSignOut={signOut}>
        {content}
      </WorkspaceLayout>
    )
  }

  return (
    <>
      <header className="sessionBar">
        <span>{user.displayName}</span>
        <button onClick={signOut}>Sign out</button>
      </header>
      <OrganizationSelectorPage />
    </>
  )
}
