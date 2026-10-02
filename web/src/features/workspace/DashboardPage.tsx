import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { organizationsApi, type Dashboard, type Role } from '../organizations/api/organizations'
import { canManageContent } from '../../shared/permissions'

export function DashboardPage() {
  const { organizationId = '' } = useParams()
  const navigate = useNavigate()
  const [data, setData] = useState<Dashboard | null>(null)
  const [role, setRole] = useState<Role>()
  const [error, setError] = useState('')
  useEffect(() => {
    Promise.all([organizationsApi.dashboard(organizationId), organizationsApi.get(organizationId)])
      .then(([dashboard, organization]) => {
        setData(dashboard)
        setRole(organization.currentUserRole)
      })
      .catch((cause) =>
        setError(cause instanceof Error ? cause.message : 'Could not load dashboard.'),
      )
  }, [organizationId])
  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">WORKSPACE</p>
          <h1>Dashboard</h1>
          <p>A simple snapshot of this organization.</p>
        </div>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <section className="statGrid" aria-label="Workspace totals">
        <article className="statCard">
          <span>Assessments</span>
          <strong>{data?.assessmentCount ?? '—'}</strong>
        </article>
        <article className="statCard">
          <span>Questions</span>
          <strong>{data?.questionCount ?? '—'}</strong>
        </article>
        <article className="statCard">
          <span>Members</span>
          <strong>{data?.memberCount ?? '—'}</strong>
        </article>
      </section>
      {canManageContent(role) && (
        <div className="inlineActions" style={{ marginTop: '1.5rem' }}>
          <button onClick={() => navigate(`/app/organizations/${organizationId}/assessments`)}>
            Open assessments
          </button>
          <button
            className="buttonSecondary"
            onClick={() => navigate(`/app/organizations/${organizationId}/questions`)}
          >
            Open question bank
          </button>
        </div>
      )}
    </>
  )
}
