import { useState, type FormEvent } from 'react'
type AssessmentFormInput = { title: string; description: string }
import styles from './AssessmentForm.module.css'

type Props = {
  initial?: AssessmentFormInput
  submitLabel: string
  busy: boolean
  onSubmit: (input: AssessmentFormInput) => Promise<void>
  onCancel: () => void
}

export function AssessmentForm({ initial, submitLabel, busy, onSubmit, onCancel }: Props) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!title.trim()) {
      setError('A title is required.')
      return
    }
    try {
      setError('')
      await onSubmit({ title: title.trim(), description })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save assessment.')
    }
  }

  return (
    <form onSubmit={submit} className={styles.form}>
      <label>
        Title <span aria-hidden="true">*</span>
        <input
          autoFocus
          required
          maxLength={200}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g. Distributed systems fundamentals"
        />
      </label>
      <label>
        Description <span className={styles.optional}>Optional</span>
        <textarea
          maxLength={2000}
          rows={5}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="What will this assessment cover?"
        />
      </label>
      {error && (
        <p className={styles.error} role="alert">
          {error}
        </p>
      )}
      <div className={styles.actions}>
        <button type="button" className="buttonSecondary" onClick={onCancel}>
          Cancel
        </button>
        <button disabled={busy} type="submit">
          {busy ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
