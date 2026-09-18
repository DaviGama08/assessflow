import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  liveApi,
  loadParticipant,
  type ParticipantState,
  type PublicQuestion,
  type QuestionResults,
} from '../api/live'
import { useLiveEvents, type LiveSocketEvent } from '../useLiveEvents'
import '../live.css'

export function ParticipantPlayPage() {
  const { sessionId = '' } = useParams()
  const stored = loadParticipant(sessionId)
  const [state, setState] = useState<ParticipantState | null>(null)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const token = stored?.token ?? null

  const refresh = useCallback(async () => {
    if (!token) return
    setState(await liveApi.state(sessionId, token))
  }, [sessionId, token])

  const onEvent = useCallback(
    (event: LiveSocketEvent) => {
      if (event.type === 'QUESTION_STARTED') {
        setState((current) =>
          current
            ? {
                ...current,
                status: 'ACTIVE',
                questionOpen: true,
                alreadyAnswered: false,
                question: event.payload as PublicQuestion,
                results: null,
              }
            : current,
        )
        setSelected([])
      } else if (event.type === 'QUESTION_RESULTS') {
        setState((current) =>
          current
            ? { ...current, questionOpen: false, results: event.payload as QuestionResults }
            : current,
        )
      } else if (event.type === 'SESSION_FINISHED' || event.type === 'SESSION_CANCELLED') {
        void refresh()
      }
    },
    [refresh],
  )
  const connection = useLiveEvents(sessionId, token, onEvent)

  useEffect(() => {
    if (!token) {
      setError('Join this session from the join page first.')
      return
    }
    refresh().catch((cause) =>
      setError(cause instanceof Error ? cause.message : 'Could not restore session.'),
    )
  }, [refresh, token])

  async function submit() {
    if (!token || selected.length === 0) return
    setBusy(true)
    try {
      await liveApi.answer(sessionId, selected, token)
      setState((current) => (current ? { ...current, alreadyAnswered: true } : current))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not submit.')
    } finally {
      setBusy(false)
    }
  }

  if (!state) {
    return (
      <main className="playPage">
        <p>{error || 'Loading…'}</p>
      </main>
    )
  }

  return (
    <main className="playPage">
      <header>
        <strong>{state.sessionName}</strong>
        <small>{connection}</small>
      </header>
      {error && <p role="alert">{error}</p>}
      {state.status === 'WAITING' && (
        <section>
          <h1>You're in!</h1>
          <p>Waiting for the instructor to start…</p>
        </section>
      )}
      {state.status === 'ACTIVE' && state.questionOpen && state.question && (
        <section>
          <p>
            {state.question.index + 1} / {state.question.total}
          </p>
          <h1>{state.question.text}</h1>
          {state.alreadyAnswered ? (
            <p>Answer submitted</p>
          ) : (
            <>
              {state.question.options.map((option) => (
                <button
                  key={option.id}
                  className={selected.includes(option.id) ? undefined : 'buttonSecondary'}
                  onClick={() =>
                    setSelected(
                      state.question?.type === 'MULTIPLE_CHOICE'
                        ? selected.includes(option.id)
                          ? selected.filter((id) => id !== option.id)
                          : [...selected, option.id]
                        : [option.id],
                    )
                  }
                >
                  {option.text}
                </button>
              ))}
              <button disabled={busy || selected.length === 0} onClick={() => void submit()}>
                Submit
              </button>
            </>
          )}
        </section>
      )}
      {state.status === 'ACTIVE' && !state.questionOpen && (
        <section>
          <h1>Waiting for the next question…</h1>
        </section>
      )}
      {(state.status === 'FINISHED' || state.status === 'CANCELLED') && (
        <section>
          {state.showResults && state.score ? (
            <>
              <h1>Session finished</h1>
              <p>
                {state.score.pointsEarned} / {state.score.pointsPossible} points (
                {Math.round(state.score.percentage)}%)
              </p>
            </>
          ) : (
            <>
              <h1>Session finished.</h1>
              <p>Thanks for participating.</p>
            </>
          )}
        </section>
      )}
    </main>
  )
}
