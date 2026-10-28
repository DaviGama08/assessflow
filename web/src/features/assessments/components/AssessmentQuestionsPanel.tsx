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
    <div className="questionComposer">
      <div>
        <div className="listToolbar">
          <h2>Questions</h2>
          <span className="statusPill">{items.length} selected</span>
        </div>
        <p className="panelDescription">
          Choose reusable questions from the bank. Points and order belong to this assessment only.
        </p>
        {items.length === 0 && <p>No questions on this assessment yet.</p>}
        {items.map((item, index) => (
          <div className="questionItem" key={item.id}>
            <div className="questionItemHeading">
              <span className="questionNumber">{String(index + 1).padStart(2, '0')}</span>
              <div>
                <strong>{item.questionText}</strong>
                <small>
                  {item.type.replaceAll('_', ' ').toLowerCase()} · {item.difficulty.toLowerCase()} ·{' '}
                  {item.status.toLowerCase()}
                </small>
              </div>
            </div>
            <div className="questionControls">
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
      </div>
      <aside className="questionLibrary">
        <h3>Question bank</h3>
        <p className="panelDescription">Add a question to this assessment.</p>
        {available.length === 0 ? (
          <p>No additional questions are available.</p>
        ) : (
          available.map((question) => (
            <div className="listRow" key={question.id}>
              <div>
                <strong>{question.text}</strong>
                <small>
                  {question.type.replaceAll('_', ' ').toLowerCase()} ·{' '}
                  {question.difficulty.toLowerCase()}
                </small>
              </div>
              <button
                className="buttonSecondary"
                disabled={busy}
                onClick={() => onAdd(question.id, 1)}
              >
                Add
              </button>
            </div>
          ))
        )}
      </aside>
    </div>
  )
}
