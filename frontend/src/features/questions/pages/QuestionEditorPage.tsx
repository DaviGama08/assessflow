import { useEffect, useState } from 'react'
import { navigate } from '../../../shared/navigation'
import { questionsApi } from '../api/questions'
import { QuestionEditor } from '../components/QuestionEditor'
import type { QuestionCategory, QuestionInput } from '../types'

export function QuestionEditorPage({
  organizationId,
  questionId,
}: {
  organizationId: string
  questionId?: string
}) {
  const [categories, setCategories] = useState<QuestionCategory[]>([])
  const [initial, setInitial] = useState<QuestionInput | undefined>()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    questionsApi
      .categories(organizationId)
      .then(async (nextCategories) => {
        setCategories(nextCategories)
        if (!questionId) return
        const question = await questionsApi.get(organizationId, questionId)
        setInitial({
          text: question.text,
          type: question.type,
          difficulty: question.difficulty,
          categoryId: question.category.id,
          explanation: question.explanation ?? '',
          status: question.status,
          options: question.options,
        })
      })
      .catch((cause) =>
        setError(cause instanceof Error ? cause.message : 'Could not load question.'),
      )
      .finally(() => setLoading(false))
  }, [organizationId, questionId])

  async function save(input: QuestionInput) {
    setBusy(true)
    try {
      if (questionId) await questionsApi.update(organizationId, questionId, input)
      else await questionsApi.create(organizationId, input)
      navigate(`/app/organizations/${organizationId}/questions`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">QUESTION BANK</p>
          <h1>{questionId ? 'Edit question' : 'New question'}</h1>
          <p>The backend validates option rules for each question type.</p>
        </div>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <section className="panel">
        {loading ? (
          <p>Loading editor…</p>
        ) : (
          <QuestionEditor
            key={questionId ?? 'new'}
            initial={initial}
            categories={categories}
            busy={busy}
            submitLabel={questionId ? 'Save question' : 'Create question'}
            onSubmit={save}
            onCancel={() => navigate(`/app/organizations/${organizationId}/questions`)}
          />
        )}
      </section>
    </>
  )
}
