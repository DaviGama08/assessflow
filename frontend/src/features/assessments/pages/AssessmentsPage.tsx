import { useCallback, useEffect, useState } from 'react'
import { assessmentsApi } from '../api/assessments'
import { AssessmentList } from '../components/AssessmentList'
import { useNavigate } from 'react-router-dom'
import type { Assessment, Page } from '../types/assessment'
import styles from './AssessmentsPage.module.css'

export function AssessmentsPage({ organizationId }: { organizationId: string }) {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Assessment> | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [title, setTitle] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setData(await assessmentsApi.list(organizationId, page))
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not load assessments.')
    } finally {
      setLoading(false)
    }
  }, [organizationId, page])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function create() {
    if (!title.trim()) return
    setBusy(true)
    try {
      const created = await assessmentsApi.create(organizationId, {
        title: title.trim(),
        description: '',
        timeLimitMinutes: null,
        maxAttempts: 1,
        passingScore: null,
        shuffleQuestions: false,
        shuffleAnswers: false,
        showResultsAfterCompletion: true,
      })
      navigate(`/app/organizations/${organizationId}/assessments/${created.id}`)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not create assessment.')
      setBusy(false)
    }
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">ASSESSMENTS</p>
          <h1>Assessments</h1>
          <p>Create and configure assessments for this organization.</p>
        </div>
      </div>
      {error && (
        <div className={styles.alert} role="alert">
          {error}
        </div>
      )}
      <section className="panel" style={{ marginBottom: '1rem' }}>
        <h2>New assessment</h2>
        <div className="filters">
          <label>
            Title
            <input
              maxLength={200}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>
          <button disabled={busy || !title.trim()} onClick={() => void create()}>
            Create assessment
          </button>
        </div>
      </section>
      <AssessmentList
        data={data}
        loading={loading}
        page={page}
        onPageChange={setPage}
        onOpen={(id) => navigate(`/app/organizations/${organizationId}/assessments/${id}`)}
        onCreate={() => undefined}
      />
    </>
  )
}
