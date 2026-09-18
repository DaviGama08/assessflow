import type { AssessmentQuestion } from '../types/assessment'
import type { Question } from '../../questions/types'

type Props = {
  items: AssessmentQuestion[]
  bank: Question[]
  busy?: boolean
  onAdd: (questionId: string, points: number) => void
  onRemove: (questionId: string) => void
  onPoints: (questionId: string, points: number) => void
  onMove: (questionId: string, direction: -1 | 1) => void
}

export function AssessmentQuestionsPanel({
  items,
  bank,
  busy,
  onAdd,
  onRemove,
  onPoints,
  onMove,
}: Props) {
  const linked = new Set(items.map((item) => item.questionId))
  const available = bank.filter(
    (question) => !linked.has(question.id) && question.status !== 'ARCHIVED',
  )
  return (
    <div>
      <h2>Questions</h2>
      <p>
        Choose reusable questions from the bank. Points and order belong to this assessment only.
      </p>
      {items.length === 0 && <p>No questions on this assessment yet.</p>}
      {items.map((item, index) => (
        <div className="listRow" key={item.id}>
          <div>
            <strong>
              {index + 1}. {item.questionText}
            </strong>
            <small>
              {item.type} · {item.difficulty} · {item.status}
            </small>
          </div>
          <div className="inlineActions">
            <label>
              Points
              <input
                type="number"
                min={1}
                value={item.points}
                aria-label={`Points for ${item.questionText}`}
                onChange={(event) => onPoints(item.questionId, Number(event.target.value))}
              />
            </label>
            <button
              className="buttonSecondary"
              disabled={busy || index === 0}
              onClick={() => onMove(item.questionId, -1)}
            >
              Up
            </button>
            <button
              className="buttonSecondary"
              disabled={busy || index === items.length - 1}
              onClick={() => onMove(item.questionId, 1)}
            >
              Down
            </button>
            <button
              className="buttonDanger"
              disabled={busy}
              onClick={() => onRemove(item.questionId)}
            >
              Remove
            </button>
          </div>
        </div>
      ))}
      <h3>Question bank</h3>
      {available.length === 0 ? (
        <p>No additional questions are available.</p>
      ) : (
        available.map((question) => (
          <div className="listRow" key={question.id}>
            <div>
              <strong>{question.text}</strong>
              <small>
                {question.type} · {question.difficulty} · {question.status}
              </small>
            </div>
            <button disabled={busy} onClick={() => onAdd(question.id, 1)}>
              Add
            </button>
          </div>
        ))
      )}
    </div>
  )
}
