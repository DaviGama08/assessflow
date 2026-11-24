import { useEffect, useState } from 'react'
import { CategoryCreator } from '../components/CategoryCreator'
import { useNavigate } from 'react-router-dom'
import { questionsApi } from '../api/questions'
import type { Question, QuestionCategory, QuestionPage } from '../types'

export function QuestionBankPage({ organizationId }: { organizationId: string }) {
  const navigate = useNavigate()
  const [data, setData] = useState<QuestionPage | null>(null)
  const [categories, setCategories] = useState<QuestionCategory[]>([])
  const [search, setSearch] = useState('')
  const [type, setType] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [status, setStatus] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  const [revision, setRevision] = useState(0)
  const reload = () => setRevision((value) => value + 1)
  useEffect(() => {
    let current = true
    Promise.all([
      questionsApi.list(organizationId, { page, search, type, difficulty, status, category }),
      questionsApi.categories(organizationId),
    ])
      .then(([questions, nextCategories]) => {
        if (current) {
          setData(questions)
          setCategories(nextCategories)
          setError('')
        }
      })
      .catch((cause) => {
        if (current) setError(cause instanceof Error ? cause.message : 'Could not load questions.')
      })
    return () => {
      current = false
    }
  }, [organizationId, page, search, type, difficulty, status, category, revision])

  async function importQuestions() {
    setError('')
    setBusy(true)
    try {
      const added = await questionsApi.installStarter(organizationId)
      setNotice(
        added
          ? `${added} questions added to the bank.`
          : 'This collection is already in your bank.',
      )
      setPage(0)
      setSearch('')
      setType('')
      setDifficulty('')
      setStatus('')
      setCategory('')
      reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not add the collection.')
    } finally {
      setBusy(false)
    }
  }

  async function archive(question: Question) {
    if (!window.confirm(`Archive “${question.text}”?`)) return
    setBusy(true)
    try {
      await questionsApi.archive(organizationId, question.id)
      reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not archive question.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">QUESTION BANK</p>
          <h1>Questions</h1>
          <p>Reusable questions for this organization. They are not tied to a single assessment.</p>
        </div>
        <button onClick={() => navigate(`/app/organizations/${organizationId}/questions/new`)}>
          New question
        </button>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <section className="panel categoryPanel">
        <h2>Categories</h2>
        {categories.length > 0 && (
          <div className="categoryTags">
            {categories.map((item) => (
              <span key={item.id}>{item.name}</span>
            ))}
          </div>
        )}
        <CategoryCreator
          organizationId={organizationId}
          onCreated={(item) => setCategories((current) => [...current, item])}
        />
      </section>
      <section className="panel">
        <h2>Starter questions</h2>
        <p>18 questions in Portuguese covering programming, databases and networks.</p>
        <button className="buttonSecondary" disabled={busy} onClick={() => void importQuestions()}>
          Add starter questions
        </button>
        {notice && <p role="status">{notice}</p>}
      </section>
      <section className="panel">
        <div className="listToolbar">
          <h2>All questions</h2>
          <span className="statusPill">{data?.totalElements ?? 0} questions</span>
        </div>
        <div className="filters">
          <label>
            Search
            <input
              value={search}
              onChange={(event) => {
                setPage(0)
                setSearch(event.target.value)
              }}
            />
          </label>
          <label>
            Type
            <select
              value={type}
              onChange={(event) => {
                setPage(0)
                setType(event.target.value)
              }}
            >
              <option value="">All</option>
              <option value="SINGLE_CHOICE">Single choice</option>
              <option value="MULTIPLE_CHOICE">Multiple choice</option>
              <option value="TRUE_FALSE">True / false</option>
            </select>
          </label>
          <label>
            Difficulty
            <select
              value={difficulty}
              onChange={(event) => {
                setPage(0)
                setDifficulty(event.target.value)
              }}
            >
              <option value="">All</option>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
            </select>
          </label>
          <label>
            Status
            <select
              value={status}
              onChange={(event) => {
                setPage(0)
                setStatus(event.target.value)
              }}
            >
              <option value="">All</option>
              <option value="DRAFT">Draft</option>
              <option value="ACTIVE">Active</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </label>
          <label>
            Category
            <select
              value={category}
              onChange={(event) => {
                setPage(0)
                setCategory(event.target.value)
              }}
            >
              <option value="">All</option>
              {categories.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        {data?.content.length ? (
          data.content.map((question) => (
            <div className="listRow" key={question.id}>
              <div>
                <strong>{question.text}</strong>
                <small>
                  {question.type.replaceAll('_', ' ').toLowerCase()} ·{' '}
                  {question.difficulty.toLowerCase()} · {question.status.toLowerCase()} ·{' '}
                  {question.category.name}
                </small>
              </div>
              <div className="inlineActions">
                <button
                  className="buttonSecondary"
                  onClick={() =>
                    navigate(`/app/organizations/${organizationId}/questions/${question.id}`)
                  }
                >
                  Edit
                </button>
                {question.status !== 'ARCHIVED' && (
                  <button
                    className="buttonDanger"
                    disabled={busy}
                    onClick={() => void archive(question)}
                  >
                    Archive
                  </button>
                )}
              </div>
            </div>
          ))
        ) : (
          <p>No questions match these filters.</p>
        )}
        <div className="inlineActions paginationBar">
          <button
            className="buttonSecondary"
            disabled={!data || data.first}
            onClick={() => setPage(page - 1)}
          >
            Previous
          </button>
          <button
            className="buttonSecondary"
            disabled={!data || data.last}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      </section>
    </>
  )
}
