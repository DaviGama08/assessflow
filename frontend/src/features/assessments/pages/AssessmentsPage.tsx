import { useCallback, useEffect, useState } from 'react'
import { assessmentsApi } from '../api/assessments'
import { AssessmentForm } from '../components/AssessmentForm'
import type { Assessment, AssessmentInput, Page } from '../types/assessment'
import styles from './AssessmentsPage.module.css'

type Mode = 'list' | 'create' | 'details' | 'edit'
const date = (value: string) => new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

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
    try { setData(await assessmentsApi.list(page)); setError('') }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not load assessments.') }
    finally { setLoading(false) }
  }, [page])
  useEffect(() => { void refresh() }, [refresh])

  async function save(input: AssessmentInput) {
    setBusy(true)
    try {
      const saved = mode === 'edit' && selected ? await assessmentsApi.update(selected.id, input) : await assessmentsApi.create(input)
      setSelected(saved); setMode('details'); await refresh()
    } finally { setBusy(false) }
  }
  async function open(id: string) {
    setLoading(true)
    try { setSelected(await assessmentsApi.get(id)); setMode('details'); setError('') }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not open assessment.') }
    finally { setLoading(false) }
  }
  async function remove() {
    if (!selected || !window.confirm(`Delete “${selected.title}”? This cannot be undone.`)) return
    setBusy(true)
    try { await assessmentsApi.delete(selected.id); setSelected(null); setMode('list'); await refresh() }
    catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not delete assessment.') }
    finally { setBusy(false) }
  }

  return <div className={styles.shell}>
    <aside className={styles.sidebar}><div className={styles.brand}><span className={styles.mark}>DQ</span><span>Distributed Quiz<small>ASSESSMENT PLATFORM</small></span></div><div className={styles.navLabel}>WORKSPACE</div><div className={styles.navActive}>▦ &nbsp; Assessments</div><div className={styles.sidebarBottom}>Foundation · Phase 1</div></aside>
    <main className={styles.main}>
      <header className={styles.topbar}><span>Workspace <span className={styles.crumb}>/ Assessments</span></span><span className={styles.topBadge}>● &nbsp; Platform ready</span></header>
      <div className={styles.content}>
        <div className={styles.heading}><div><p className={styles.eyebrow}>YOUR WORKSPACE</p><h1>Assessments</h1><p className={styles.subtitle}>Create and manage the foundations of your learning experiences.</p></div><button onClick={() => { setSelected(null); setMode('create') }}>+ &nbsp; New assessment</button></div>
        {error && <div className={styles.alert} role="alert">{error} <button className="buttonSecondary" onClick={() => void refresh()}>Retry</button></div>}
        {mode === 'create' || mode === 'edit' ? <section className={styles.panel}><div className={styles.panelHeader}><h2>{mode === 'create' ? 'Create assessment' : 'Edit assessment'}</h2><p>Give your assessment a clear name and optional description.</p></div><AssessmentForm key={`${mode}-${selected?.id ?? ''}`} initial={mode === 'edit' && selected ? { title: selected.title, description: selected.description ?? '' } : undefined} submitLabel={mode === 'create' ? 'Create assessment' : 'Save changes'} busy={busy} onSubmit={save} onCancel={() => setMode(selected ? 'details' : 'list')} /></section>
        : mode === 'details' && selected ? <section className={styles.panel}><button className={styles.back} onClick={() => setMode('list')}>← All assessments</button><div className={styles.detailTitle}><div><span className={styles.status}>{selected.status}</span><h2>{selected.title}</h2></div><div className={styles.detailActions}><button className="buttonSecondary" onClick={() => setMode('edit')}>Edit</button><button className="buttonDanger" disabled={busy} onClick={() => void remove()}>Delete</button></div></div><p className={styles.description}>{selected.description || 'No description added yet.'}</p><div className={styles.metadata}><div><small>CREATED</small><strong>{date(selected.createdAt)}</strong></div><div><small>LAST UPDATED</small><strong>{date(selected.updatedAt)}</strong></div><div><small>ASSESSMENT ID</small><strong className={styles.id}>{selected.id}</strong></div></div></section>
        : <section className={styles.panel}><div className={styles.listHead}><div><h2>All assessments</h2><p>{data?.totalElements ?? 0} total assessments</p></div></div>{loading ? <p className={styles.empty}>Loading assessments…</p> : data?.content.length ? <><div className={styles.tableWrap}><table><thead><tr><th>ASSESSMENT</th><th>STATUS</th><th>CREATED</th><th>UPDATED</th><th></th></tr></thead><tbody>{data.content.map(item => <tr key={item.id}><td><button className={styles.titleButton} onClick={() => void open(item.id)}>{item.title}</button><small>{item.description || 'No description'}</small></td><td><span className={styles.status}>{item.status}</span></td><td>{date(item.createdAt)}</td><td>{date(item.updatedAt)}</td><td><button className={styles.arrow} aria-label={`Open ${item.title}`} onClick={() => void open(item.id)}>↗</button></td></tr>)}</tbody></table></div><div className={styles.pagination}><span>Page {page + 1} of {data.totalPages}</span><div><button className="buttonSecondary" disabled={data.first} onClick={() => setPage(page - 1)}>Previous</button><button className="buttonSecondary" disabled={data.last} onClick={() => setPage(page + 1)}>Next</button></div></div></> : <div className={styles.empty}><div className={styles.emptyIcon}>▦</div><h3>No assessments yet</h3><p>Create your first assessment to get started.</p><button onClick={() => setMode('create')}>Create assessment</button></div>}</section>}
      </div>
    </main>
  </div>
}
