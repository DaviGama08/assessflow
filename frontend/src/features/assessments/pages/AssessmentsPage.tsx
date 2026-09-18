import { useCallback, useEffect, useState } from 'react'
import { assessmentsApi } from '../api/assessments'
import { AssessmentDetails } from '../components/AssessmentDetails'
import { AssessmentForm } from '../components/AssessmentForm'
import { AssessmentList } from '../components/AssessmentList'
import type { Assessment, AssessmentInput, Page } from '../types/assessment'
import styles from './AssessmentsPage.module.css'

type Mode = 'list' | 'create' | 'details' | 'edit'

export function AssessmentsPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Assessment> | null>(null)
  const [selected, setSelected] = useState<Assessment | null>(null)
  const [mode, setMode] = useState<Mode>('list')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setData(await assessmentsApi.list(page))
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not load assessments.')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function save(input: AssessmentInput) {
    setBusy(true)
    try {
      const saved =
        mode === 'edit' && selected
          ? await assessmentsApi.update(selected.id, input)
          : await assessmentsApi.create(input)
      setSelected(saved)
      setMode('details')
      await refresh()
    } finally {
      setBusy(false)
    }
  }

  async function open(id: string) {
    setLoading(true)
    try {
      setSelected(await assessmentsApi.get(id))
      setMode('details')
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not open assessment.')
    } finally {
      setLoading(false)
    }
  }

  async function remove() {
    if (!selected || !window.confirm(`Delete “${selected.title}”? This cannot be undone.`)) return
    setBusy(true)
    try {
      await assessmentsApi.delete(selected.id)
      setSelected(null)
      setMode('list')
      await refresh()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not delete assessment.')
    } finally {
      setBusy(false)
    }
  }

  function startCreate() {
    setSelected(null)
    setMode('create')
  }

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.mark}>AF</span>
          <span>
            AssessFlow<small>ASSESSMENT PLATFORM</small>
          </span>
        </div>
        <div className={styles.navLabel}>WORKSPACE</div>
        <div className={styles.navActive}>▦ &nbsp; Assessments</div>
        <div className={styles.sidebarBottom}>Foundation · Phase 1</div>
      </aside>
      <main className={styles.main}>
        <header className={styles.topbar}>
          <span>
            Workspace <span className={styles.crumb}>/ Assessments</span>
          </span>
          <span className={styles.topBadge}>● &nbsp; Platform ready</span>
        </header>
        <div className={styles.content}>
          <div className={styles.heading}>
            <div>
              <p className={styles.eyebrow}>YOUR WORKSPACE</p>
              <h1>Assessments</h1>
              <p className={styles.subtitle}>
                Create and manage the foundations of your learning experiences.
              </p>
            </div>
            <button onClick={startCreate}>+ &nbsp; New assessment</button>
          </div>
          {error && (
            <div className={styles.alert} role="alert">
              {error}{' '}
              <button className="buttonSecondary" onClick={() => void refresh()}>
                Retry
              </button>
            </div>
          )}
          {mode === 'create' || mode === 'edit' ? (
            <section className={styles.panel}>
              <div className={styles.panelHeader}>
                <h2>{mode === 'create' ? 'Create assessment' : 'Edit assessment'}</h2>
                <p>Give your assessment a clear name and optional description.</p>
              </div>
              <AssessmentForm
                key={`${mode}-${selected?.id ?? ''}`}
                initial={
                  mode === 'edit' && selected
                    ? { title: selected.title, description: selected.description ?? '' }
                    : undefined
                }
                submitLabel={mode === 'create' ? 'Create assessment' : 'Save changes'}
                busy={busy}
                onSubmit={save}
                onCancel={() => setMode(selected ? 'details' : 'list')}
              />
            </section>
          ) : mode === 'details' && selected ? (
            <AssessmentDetails
              assessment={selected}
              busy={busy}
              onBack={() => setMode('list')}
              onEdit={() => setMode('edit')}
              onDelete={() => void remove()}
            />
          ) : (
            <AssessmentList
              data={data}
              loading={loading}
              page={page}
              onPageChange={setPage}
              onOpen={(id) => void open(id)}
              onCreate={startCreate}
            />
          )}
        </div>
      </main>
    </div>
  )
}
