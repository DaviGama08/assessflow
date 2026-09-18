import { Navigate, Outlet, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { GuestOnly, RequireAuth, RequireOrganizationRole } from './guards'
import { useAuth } from '../features/auth/AuthContext'
import { LoginPage, RegisterPage } from '../features/auth/pages/AuthPages'
import { AssessmentsPage } from '../features/assessments/pages/AssessmentsPage'
import { AssessmentBuilderPage } from '../features/assessments/pages/AssessmentBuilderPage'
import {
  MembersPage,
  OrganizationSelectorPage,
} from '../features/organizations/pages/OrganizationPages'
import { QuestionBankPage } from '../features/questions/pages/QuestionBankPage'
import { QuestionEditorPage } from '../features/questions/pages/QuestionEditorPage'
import { DashboardPage } from '../features/workspace/DashboardPage'
import { OrganizationProvider } from '../features/workspace/OrganizationContext'
import { SettingsPage } from '../features/workspace/SettingsPage'
import { HostLivePage } from '../features/live/pages/HostLivePage'
import { LocalLivePage } from '../features/live/pages/LocalLivePage'
import { JoinCodePage, JoinPage } from '../features/live/pages/JoinPages'
import { ParticipantPlayPage } from '../features/live/pages/ParticipantPlayPage'
import { WorkspaceLayout } from '../features/workspace/WorkspaceLayout'

const contentRoles = ['OWNER', 'ADMIN', 'INSTRUCTOR'] as const
const managerRoles = ['OWNER', 'ADMIN'] as const

function AuthenticatedShell() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  return (
    <>
      <header className="sessionBar">
        <span>{user?.displayName}</span>
        <button
          onClick={() => {
            void signOut().then(() => navigate('/login'))
          }}
        >
          Sign out
        </button>
      </header>
      <Outlet />
    </>
  )
}

function WorkspaceRoutes() {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  if (!user) return <Navigate to="/login" replace />
  return (
    <OrganizationProvider>
      <WorkspaceLayout
        user={user}
        onSignOut={() => {
          void signOut().then(() => navigate('/login'))
        }}
      />
    </OrganizationProvider>
  )
}

function ParamAssessmentsPage() {
  const { organizationId = '' } = useParams()
  return (
    <RequireOrganizationRole roles={[...contentRoles]}>
      <AssessmentsPage organizationId={organizationId} />
    </RequireOrganizationRole>
  )
}

function ParamAssessmentBuilderPage() {
  const { organizationId = '', assessmentId = '' } = useParams()
  return (
    <RequireOrganizationRole roles={[...contentRoles]}>
      <AssessmentBuilderPage organizationId={organizationId} assessmentId={assessmentId} />
    </RequireOrganizationRole>
  )
}

function ParamQuestionBankPage() {
  const { organizationId = '' } = useParams()
  return (
    <RequireOrganizationRole roles={[...contentRoles]}>
      <QuestionBankPage organizationId={organizationId} />
    </RequireOrganizationRole>
  )
}

function ParamQuestionEditorPage() {
  const { organizationId = '', questionId } = useParams()
  return (
    <RequireOrganizationRole roles={[...contentRoles]}>
      <QuestionEditorPage
        organizationId={organizationId}
        questionId={questionId === 'new' ? undefined : questionId}
      />
    </RequireOrganizationRole>
  )
}

function ParamMembersPage() {
  const { organizationId = '' } = useParams()
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return <MembersPage id={organizationId} user={user} />
}

function ParamSettingsPage() {
  const { organizationId = '' } = useParams()
  return (
    <RequireOrganizationRole roles={[...managerRoles]}>
      <SettingsPage organizationId={organizationId} />
    </RequireOrganizationRole>
  )
}

function ParamHostLivePage() {
  return (
    <RequireOrganizationRole roles={[...contentRoles]}>
      <HostLivePage />
    </RequireOrganizationRole>
  )
}

export function App() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  function authenticated(user: Parameters<typeof signIn>[0]) {
    signIn(user)
    void navigate('/app', { replace: true })
  }
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <GuestOnly>
            <LoginPage onAuthenticated={authenticated} />
          </GuestOnly>
        }
      />
      <Route
        path="/register"
        element={
          <GuestOnly>
            <RegisterPage onAuthenticated={authenticated} />
          </GuestOnly>
        }
      />
      <Route path="/join" element={<JoinPage />} />
      <Route path="/join/:code" element={<JoinCodePage />} />
      <Route path="/play/:sessionId" element={<ParticipantPlayPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AuthenticatedShell />}>
          <Route path="/app" element={<OrganizationSelectorPage />} />
        </Route>
        <Route path="/app/organizations/:organizationId" element={<WorkspaceRoutes />}>
          <Route index element={<DashboardPage />} />
          <Route path="assessments" element={<ParamAssessmentsPage />} />
          <Route path="assessments/:assessmentId" element={<ParamAssessmentBuilderPage />} />
          <Route path="questions" element={<ParamQuestionBankPage />} />
          <Route path="questions/:questionId" element={<ParamQuestionEditorPage />} />
          <Route path="members" element={<ParamMembersPage />} />
          <Route path="settings" element={<ParamSettingsPage />} />
          <Route path="live-sessions/:sessionId" element={<ParamHostLivePage />} />
          <Route
            path="local-live"
            element={
              <RequireOrganizationRole roles={[...contentRoles]}>
                <LocalLivePage />
              </RequireOrganizationRole>
            }
          />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}
