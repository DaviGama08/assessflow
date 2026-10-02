import { useMemo, useState, type FormEvent } from 'react'
import type {
  AnswerOption,
  QuestionCategory,
  QuestionDifficulty,
  QuestionInput,
  QuestionType,
} from '../types'

type Props = {
  initial?: QuestionInput
  categories: QuestionCategory[]
  busy?: boolean
  submitLabel: string
  onSubmit: (input: QuestionInput) => Promise<void>
  onCancel: () => void
}

const emptyOption = (): AnswerOption => ({ text: '', correct: false })

export function QuestionEditor({
  initial,
  categories,
  busy,
  submitLabel,
  onSubmit,
  onCancel,
}: Props) {
  const [text, setText] = useState(initial?.text ?? '')
  const [type, setType] = useState<QuestionType>(initial?.type ?? 'SINGLE_CHOICE')
  const [difficulty, setDifficulty] = useState<QuestionDifficulty>(initial?.difficulty ?? 'MEDIUM')
  const [categoryId, setCategoryId] = useState(initial?.categoryId ?? categories[0]?.id ?? '')
  const [explanation, setExplanation] = useState(initial?.explanation ?? '')
  const [options, setOptions] = useState<AnswerOption[]>(
    initial?.options?.length
      ? initial.options
      : type === 'TRUE_FALSE'
        ? [
            { text: 'True', correct: true },
            { text: 'False', correct: false },
          ]
        : [emptyOption(), emptyOption()],
  )
  const [error, setError] = useState('')
  const multiple = type === 'MULTIPLE_CHOICE'

  const optionHint = useMemo(() => {
    if (type === 'SINGLE_CHOICE') return 'Add at least two options and mark exactly one as correct.'
    if (type === 'MULTIPLE_CHOICE')
      return 'Add at least two options and mark one or more as correct.'
    return 'True/false questions use exactly two options and one correct answer.'
  }, [type])

  function changeType(next: QuestionType) {
    setType(next)
    if (next === 'TRUE_FALSE') {
      setOptions([
        { text: options[0]?.text || 'True', correct: true },
        { text: options[1]?.text || 'False', correct: false },
      ])
    }
  }

  function markCorrect(index: number) {
    setOptions((current) =>
      current.map((option, optionIndex) => ({
        ...option,
        correct: multiple
          ? optionIndex === index
            ? !option.correct
            : option.correct
          : optionIndex === index,
      })),
    )
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!text.trim()) {
      setError('Question text is required.')
      return
    }
    if (!categoryId) {
      setError('A category is required.')
      return
    }
    try {
      setError('')
      await onSubmit({
        text: text.trim(),
        type,
        difficulty,
        categoryId,
        explanation,
        options: options.map((option, index) => ({
          text: option.text,
          correct: option.correct,
          displayOrder: index,
        })),
      })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save question.')
    }
  }

  return (
    <form className="formGrid" onSubmit={submit}>
      <label>
        Question text
        <textarea
          required
          maxLength={4000}
          rows={4}
          value={text}
          onChange={(event) => setText(event.target.value)}
        />
      </label>
      <label>
        Type
        <select value={type} onChange={(event) => changeType(event.target.value as QuestionType)}>
          <option value="SINGLE_CHOICE">Single choice</option>
          <option value="MULTIPLE_CHOICE">Multiple choice</option>
          <option value="TRUE_FALSE">True / false</option>
        </select>
      </label>
      <label>
        Difficulty
        <select
          value={difficulty}
          onChange={(event) => setDifficulty(event.target.value as QuestionDifficulty)}
        >
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
      </label>
      <label>
        Category
        <select required value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
          <option value="">Select a category</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        Explanation
        <textarea
          maxLength={4000}
          rows={3}
          value={explanation}
          onChange={(event) => setExplanation(event.target.value)}
        />
      </label>
      <fieldset>
        <legend>Options</legend>
        <p>{optionHint}</p>
        {options.map((option, index) => (
          <div className="optionRow" key={index}>
            <label>
              Option {index + 1}
              <input
                required
                maxLength={2000}
                value={option.text}
                onChange={(event) =>
                  setOptions((current) =>
                    current.map((item, itemIndex) =>
                      itemIndex === index ? { ...item, text: event.target.value } : item,
                    ),
                  )
                }
              />
            </label>
            <label>
              <input
                type={multiple ? 'checkbox' : 'radio'}
                name="correct-option"
                checked={option.correct}
                onChange={() => markCorrect(index)}
              />
              Correct
            </label>
            {type !== 'TRUE_FALSE' && options.length > 2 && (
              <button
                type="button"
                className="buttonDanger"
                onClick={() =>
                  setOptions((current) => current.filter((_, itemIndex) => itemIndex !== index))
                }
              >
                Remove
              </button>
            )}
          </div>
        ))}
        {type !== 'TRUE_FALSE' && (
          <button
            type="button"
            className="buttonSecondary"
            onClick={() => setOptions((current) => [...current, emptyOption()])}
          >
            Add option
          </button>
        )}
      </fieldset>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <div className="inlineActions">
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
