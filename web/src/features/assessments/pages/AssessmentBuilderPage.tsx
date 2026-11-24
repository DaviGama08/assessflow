import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { assessmentsApi } from '../api/assessments'
import { liveApi } from '../../live/api/live'
import { questionsApi } from '../../questions/api/questions'
import { AssessmentQuestionsPanel } from '../components/AssessmentQuestionsPanel'
import type { Assessment, AssessmentQuestion } from '../types/assessment'
import type { QuestionPage } from '../../questions/types'

type Tab = 'general' | 'questions' | 'settings'

export function AssessmentBuilderPage({
  organizationId,
  assessmentId,
}: {
  organizationId: string
  assessmentId: string
}) {
  const navigate = useNavigate()
  const [tab, setTab] = useState<Tab>('general')
  const [assessment, setAssessment] = useState<Assessment | null>(null)
  const [items, setItems] = useState<AssessmentQuestion[]>([])
  const [bank, setBank] = useState<QuestionPage | null>(null)
  const [bankPage, setBankPage] = useState(0)
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [timeLimitMinutes, setTimeLimitMinutes] = useState('')
  const [maxAttempts, setMaxAttempts] = useState('1')
  const [passingScore, setPassingScore] = useState('')
  const [shuffleQuestions, setShuffleQuestions] = useState(false)
  const [shuffleAnswers, setShuffleAnswers] = useState(false)
  const [showResultsAfterCompletion, setShowResultsAfterCompletion] = useState(true)

  async function reload() {
    const [nextAssessment, nextItems] = await Promise.all([
      assessmentsApi.get(organizationId, assessmentId),
      assessmentsApi.questions(organizationId, assessmentId),
    ])
    setAssessment(nextAssessment)
    setItems(nextItems)
    setTitle(nextAssessment.title)
    setDescription(nextAssessment.description ?? '')
    setTimeLimitMinutes(nextAssessment.timeLimitMinutes?.toString() ?? '')
    setMaxAttempts(String(nextAssessment.maxAttempts))
    setPassingScore(nextAssessment.passingScore?.toString() ?? '')
    setShuffleQuestions(nextAssessment.shuffleQuestions)
    setShuffleAnswers(nextAssessment.shuffleAnswers)
    setShowResultsAfterCompletion(nextAssessment.showResultsAfterCompletion)
  }

  useEffect(() => {
    reload().catch((cause) =>
      setError(cause instanceof Error ? cause.message : 'Could not load assessment.'),
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reload is recreated each render
  }, [organizationId, assessmentId])

  useEffect(() => {
    if (tab !== 'questions') return
    let current = true
    setBank(null)
    questionsApi
      .list(organizationId, { status: 'ACTIVE', page: bankPage, search })
      .then((result) => {
        if (current) setBank(result)
      })
      .catch((cause) => {
        if (current)
          setError(cause instanceof Error ? cause.message : 'Could not load the question bank.')
      })
    return () => {
      current = false
    }
  }, [organizationId, bankPage, search, tab])

  async function publish() {
    setBusy(true)
    setError('')
    try {
      await assessmentsApi.publish(organizationId, assessmentId)
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not publish assessment.')
    } finally {
      setBusy(false)
    }
  }

  async function archive() {
    setBusy(true)
    setError('')
    try {
      await assessmentsApi.archive(organizationId, assessmentId)
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not archive assessment.')
    } finally {
      setBusy(false)
    }
  }

  async function exportPackage() {
    setBusy(true)
    setError('')
    try {
      const pack = await assessmentsApi.exportPackage(organizationId, assessmentId)
      const blob = new Blob([JSON.stringify(pack, null, 2)], { type: 'application/json' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `${assessment?.title ?? 'assessment'}.assessflow.json`
      link.click()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not export the package.')
    } finally {
      setBusy(false)
    }
  }

  async function startLive() {
    setBusy(true)
    setError('')
    try {
      const session = await liveApi.create(organizationId, assessmentId)
      navigate(`/app/organizations/${organizationId}/live-sessions/${session.id}`)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not start a live session.')
      setBusy(false)
    }
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await assessmentsApi.update(organizationId, assessmentId, {
        title,
        description,
        timeLimitMinutes: timeLimitMinutes ? Number(timeLimitMinutes) : null,
        maxAttempts: Number(maxAttempts),
        passingScore: passingScore ? Number(passingScore) : null,
        shuffleQuestions,
        shuffleAnswers,
        showResultsAfterCompletion,
      })
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save assessment.')
    } finally {
      setBusy(false)
    }
  }

  async function add(questionId: string, points: number) {
    setBusy(true)
    try {
      await assessmentsApi.addQuestion(organizationId, assessmentId, questionId, points)
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not add question.')
    } finally {
      setBusy(false)
    }
  }

  async function remove(questionId: string) {
    setBusy(true)
    try {
      await assessmentsApi.removeQuestion(organizationId, assessmentId, questionId)
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not remove question.')
    } finally {
      setBusy(false)
    }
  }

  async function points(questionId: string, value: number) {
    if (value < 1) return
    setBusy(true)
    try {
      await assessmentsApi.updateQuestionPoints(organizationId, assessmentId, questionId, value)
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not update points.')
    } finally {
      setBusy(false)
    }
  }

  async function move(questionId: string, direction: -1 | 1) {
    const ids = items.map((item) => item.questionId)
    const index = ids.indexOf(questionId)
    const next = index + direction
    if (index < 0 || next < 0 || next >= ids.length) return
    const reordered = [...ids]
    const [moved] = reordered.splice(index, 1)
    reordered.splice(next, 0, moved)
    setBusy(true)
    try {
      setItems(await assessmentsApi.reorderQuestions(organizationId, assessmentId, reordered))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not reorder questions.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">ASSESSMENT BUILDER</p>
          <h1>{assessment?.title ?? 'Assessment'}</h1>
          <p>
            Status: {assessment?.status ?? 'DRAFT'}. General details, question selection and
            delivery settings.
          </p>
        </div>
        <div className="inlineActions">
          {assessment?.status === 'DRAFT' && (
            <button
              disabled={busy}
              onClick={() => (items.length ? void publish() : setTab('questions'))}
            >
              {items.length ? 'Publish' : 'Choose questions'}
            </button>
          )}
          {assessment?.status === 'PUBLISHED' && (
            <>
              <button disabled={busy} onClick={() => void startLive()}>
                Start live session
              </button>
              <button
                className="buttonSecondary"
                disabled={busy}
                onClick={() => void exportPackage()}
              >
                Export package
              </button>
            </>
          )}
          {assessment && assessment.status !== 'ARCHIVED' && (
            <button className="buttonSecondary" disabled={busy} onClick={() => void archive()}>
              Archive
            </button>
          )}
        </div>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <div className="tabs" role="tablist" aria-label="Assessment sections">
        {(['general', 'questions', 'settings'] as Tab[]).map((item) => (
          <button
            key={item}
            role="tab"
            id={`assessment-tab-${item}`}
            aria-controls={`assessment-panel-${item}`}
            tabIndex={tab === item ? 0 : -1}
            aria-selected={tab === item}
            onKeyDown={(event) => {
              const tabs: Tab[] = ['general', 'questions', 'settings']
              const index = tabs.indexOf(item)
              const next =
                event.key === 'ArrowRight'
                  ? tabs[(index + 1) % tabs.length]
                  : event.key === 'ArrowLeft'
                    ? tabs[(index + tabs.length - 1) % tabs.length]
                    : event.key === 'Home'
                      ? tabs[0]
                      : event.key === 'End'
                        ? tabs[tabs.length - 1]
                        : null
              if (next) {
                event.preventDefault()
                setTab(next)
                document.getElementById(`assessment-tab-${next}`)?.focus()
              }
            }}
            className={tab === item ? undefined : 'buttonSecondary'}
            onClick={() => setTab(item)}
          >
            {item[0].toUpperCase() + item.slice(1)}
          </button>
        ))}
      </div>
      <section
        className="panel"
        role="tabpanel"
        id={`assessment-panel-${tab}`}
        aria-labelledby={`assessment-tab-${tab}`}
      >
        {tab === 'questions' ? (
          <>
            <AssessmentQuestionsPanel
              items={items}
              bank={bank?.content ?? []}
              readOnly={assessment?.status === 'ARCHIVED'}
              libraryTools={
                <>
                  <label>
                    Search active questions
                    <input
                      value={search}
                      onChange={(event) => {
                        setSearch(event.target.value)
                        setBankPage(0)
                      }}
                    />
                  </label>
                  <Link to={`/app/organizations/${organizationId}/questions`}>
                    Open question bank
                  </Link>
                </>
              }
              libraryFooter={
                bank &&
                bank.totalPages > 1 && (
                  <div className="inlineActions">
                    <button disabled={bank.first} onClick={() => setBankPage((p) => p - 1)}>
                      Previous
                    </button>
                    <span>
                      {bank.number + 1} / {bank.totalPages}
                    </span>
                    <button disabled={bank.last} onClick={() => setBankPage((p) => p + 1)}>
                      Next
                    </button>
                  </div>
                )
              }
              loading={!bank}
              busy={busy}
              onAdd={(id, value) => void add(id, value)}
              onRemove={(id) => void remove(id)}
              onPoints={(id, value) => void points(id, value)}
              onMove={(id, direction) => void move(id, direction)}
            />
          </>
        ) : (
          <form onSubmit={save}>
            <fieldset
              className="formGrid assessmentFields"
              disabled={busy || assessment?.status === 'ARCHIVED'}
            >
              {tab === 'general' && (
                <>
                  <label>
                    Title
                    <input
                      required
                      maxLength={200}
                      value={title}
                      onChange={(event) => setTitle(event.target.value)}
                    />
                  </label>
                  <label>
                    Description
                    <textarea
                      maxLength={2000}
                      rows={5}
                      value={description}
                      onChange={(event) => setDescription(event.target.value)}
                    />
                  </label>
                </>
              )}
              {tab === 'settings' && (
                <>
                  <label>
                    Time limit (minutes)
                    <input
                      type="number"
                      min={1}
                      value={timeLimitMinutes}
                      onChange={(event) => setTimeLimitMinutes(event.target.value)}
                    />
                  </label>
                  <label>
                    Maximum attempts
                    <input
                      type="number"
                      min={1}
                      required
                      value={maxAttempts}
                      onChange={(event) => setMaxAttempts(event.target.value)}
                    />
                  </label>
                  <label>
                    Passing score (0-100)
                    <input
                      type="number"
                      min={0}
                      max={100}
                      value={passingScore}
                      onChange={(event) => setPassingScore(event.target.value)}
                    />
                  </label>
                  <label>
                    <input
                      type="checkbox"
                      checked={shuffleQuestions}
                      onChange={(event) => setShuffleQuestions(event.target.checked)}
                    />
                    Shuffle questions
                  </label>
                  <label>
                    <input
                      type="checkbox"
                      checked={shuffleAnswers}
                      onChange={(event) => setShuffleAnswers(event.target.checked)}
                    />
                    Shuffle answers
                  </label>
                  <label>
                    <input
                      type="checkbox"
                      checked={showResultsAfterCompletion}
                      onChange={(event) => setShowResultsAfterCompletion(event.target.checked)}
                    />
                    Show results after completion
                  </label>
                </>
              )}
              <div>
                <button disabled={busy} type="submit">
                  {busy ? 'Saving…' : 'Save'}
                </button>
              </div>
            </fieldset>
          </form>
        )}
      </section>
    </>
  )
}
