import { useEffect, useState, type FormEvent } from 'react'
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
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [busy, setBusy] = useState(false)

  async function reload(nextPage = page) {
    const [questions, nextCategories] = await Promise.all([
      questionsApi.list(organizationId, {
        page: nextPage,
        search,
        type,
        difficulty,
        status,
        category,
      }),
      questionsApi.categories(organizationId),
    ])
    setData(questions)
    setCategories(nextCategories)
  }

  useEffect(() => {
    reload(page).catch((cause) =>
      setError(cause instanceof Error ? cause.message : 'Could not load questions.'),
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reload is recreated each render
  }, [organizationId, page, search, type, difficulty, status, category])

  async function createCategory(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await questionsApi.createCategory(organizationId, name, slug)
      setName('')
      setSlug('')
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not create category.')
    } finally {
      setBusy(false)
    }
  }

  async function archive(question: Question) {
    if (!window.confirm(`Archive “${question.text}”?`)) return
    setBusy(true)
    try {
      await questionsApi.archive(organizationId, question.id)
      await reload()
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
      <section className="panel">
        <h2>Categories</h2>
        <div className="filters">
          {categories.map((item) => (
            <span key={item.id}>
              {item.name} <small>({item.slug})</small>
            </span>
          ))}
        </div>
        <form className="filters" onSubmit={createCategory}>
          <label>
            Name
            <input
              required
              maxLength={200}
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          <label>
            Slug
            <input
              required
              maxLength={100}
              value={slug}
              onChange={(event) => setSlug(event.target.value)}
            />
          </label>
          <button disabled={busy} type="submit">
            Add category
          </button>
        </form>
      </section>
      <section className="panel" style={{ marginTop: '1rem' }}>
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
                  {question.type} · {question.difficulty} · {question.status} ·{' '}
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
        <div className="inlineActions" style={{ marginTop: '1rem' }}>
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
