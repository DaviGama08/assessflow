import { useState } from 'react'
import { questionsApi } from '../api/questions'
import type { QuestionCategory } from '../types'

export function CategoryCreator({
  organizationId,
  onCreated,
}: {
  organizationId: string
  onCreated: (category: QuestionCategory) => void
}) {
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function create() {
    if (busy || !name.trim()) return
    setBusy(true)
    setError('')
    try {
      const category = await questionsApi.createCategory(organizationId, name.trim(), name.trim())
      onCreated(category)
      setName('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not create category.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <div>
      <div className="filters">
        <label>
          New category name
          <input
            maxLength={100}
            value={name}
            onChange={(event) => setName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault()
                void create()
              }
            }}
          />
        </label>
        <button
          className="buttonSecondary"
          type="button"
          disabled={busy || !name.trim()}
          onClick={() => void create()}
        >
          {busy ? 'Adding…' : 'Add category'}
        </button>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
