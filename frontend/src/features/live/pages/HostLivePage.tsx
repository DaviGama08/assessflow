import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getAccessToken } from '../../auth/api/auth'
import { QrCode } from '../components/QrCode'
import { joinUrl } from '../joinUrl'
import {
  liveApi,
  type LiveParticipant,
  type LiveSession,
  type PublicQuestion,
  type QuestionResults,
} from '../api/live'
import { useLiveEvents, type LiveSocketEvent } from '../useLiveEvents'
import '../live.css'

export function HostLivePage() {
  const { organizationId = '', sessionId = '' } = useParams()
  const [session, setSession] = useState<LiveSession | null>(null)
  const [people, setPeople] = useState<LiveParticipant[]>([])
  const [question, setQuestion] = useState<PublicQuestion | null>(null)
  const [results, setResults] = useState<QuestionResults | null>(null)
  const [answered, setAnswered] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const token = getAccessToken()

  const reload = useCallback(async () => {
    const [nextSession, nextPeople] = await Promise.all([
      liveApi.get(organizationId, sessionId),
      liveApi.participants(organizationId, sessionId),
    ])
    setSession(nextSession)
    setPeople(nextPeople)
    if (nextSession.currentQuestionIndex != null) {
      setQuestion(await liveApi.currentQuestion(organizationId, sessionId).catch(() => null))
      if (!nextSession.questionOpen) {
        setResults(await liveApi.results(organizationId, sessionId).catch(() => null))
      }
    }
  }, [organizationId, sessionId])

  const onEvent = useCallback(
    (event: LiveSocketEvent) => {
      if (
        event.type === 'PARTICIPANT_JOINED' ||
        event.type === 'PARTICIPANT_LEFT' ||
        event.type === 'PRESENCE_CHANGED'
      )
        void reload()
      if (event.type === 'QUESTION_STARTED') {
        setQuestion(event.payload as PublicQuestion)
        setResults(null)
        setAnswered(0)
        setSession((current) =>
          current ? { ...current, status: 'ACTIVE', questionOpen: true } : current,
        )
      }
      if (event.type === 'ANSWER_RECEIVED') {
        const payload = event.payload as { answered: number }
        setAnswered(payload.answered)
      }
      if (event.type === 'QUESTION_RESULTS') {
        setSession((current) => (current ? { ...current, questionOpen: false } : current))
        void liveApi
          .results(organizationId, sessionId)
          .then(setResults)
          .catch(() => undefined)
      }
      if (event.type === 'SESSION_FINISHED' || event.type === 'SESSION_CANCELLED') void reload()
    },
    [organizationId, reload, sessionId],
  )
  const connection = useLiveEvents(sessionId, token, onEvent, 'host')

  useEffect(() => {
    reload().catch((cause) =>
      setError(cause instanceof Error ? cause.message : 'Could not load session.'),
    )
  }, [reload])

  async function run(action: () => Promise<unknown>) {
    setBusy(true)
    setError('')
    try {
      await action()
      await reload()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The command failed.')
    } finally {
      setBusy(false)
    }
  }

  if (!session) return <p>{error || 'Loading session…'}</p>
  const url = joinUrl(session.joinCode)

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">LIVE SESSION</p>
          <h1>{session.assessmentTitle}</h1>
          <p>
            Status: {session.status} · {connection}
          </p>
        </div>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <section className="panel">
        <h2>Code {session.joinCode}</h2>
        <p>{url}</p>
        <div className="hostQr">
          <QrCode value={url} />
        </div>
        <p>{people.length} participants</p>
        {session.status === 'WAITING' && (
          <button
            disabled={busy}
            onClick={() => void run(() => liveApi.start(organizationId, sessionId))}
          >
            Start Session
          </button>
        )}
        {session.status === 'ACTIVE' && session.questionOpen && (
          <button
            disabled={busy}
            onClick={() => void run(() => liveApi.endQuestion(organizationId, sessionId))}
          >
            End question
          </button>
        )}
        {session.status === 'ACTIVE' && !session.questionOpen && (
          <div className="inlineActions">
            {question && question.index + 1 < question.total && (
              <button
                disabled={busy}
                onClick={() => void run(() => liveApi.nextQuestion(organizationId, sessionId))}
              >
                Next question
              </button>
            )}
            <button
              className="buttonSecondary"
              disabled={busy}
              onClick={() => void run(() => liveApi.finish(organizationId, sessionId))}
            >
              Finish
            </button>
          </div>
        )}
        {session.status === 'WAITING' && (
          <button
            className="buttonDanger"
            disabled={busy}
            onClick={() => void run(() => liveApi.cancel(organizationId, sessionId))}
          >
            Cancel
          </button>
        )}
      </section>
      {question && (
        <section className="panel" style={{ marginTop: '1rem' }}>
          <h2>
            Question {question.index + 1} / {question.total}
          </h2>
          <p>{question.text}</p>
          {session.questionOpen && (
            <p>
              {answered} / {people.length} answered
            </p>
          )}
          {results &&
            results.options.map((option) => (
              <p key={option.id}>
                {option.text}: {Math.round(option.percent)}%{option.correct ? ' ✓' : ''}
              </p>
            ))}
        </section>
      )}
      <section className="panel" style={{ marginTop: '1rem' }}>
        <h2>Participants</h2>
        {people.map((person) => (
          <div className="listRow" key={person.id}>
            <strong>{person.displayName}</strong>
            <small>{person.status}</small>
          </div>
        ))}
      </section>
    </>
  )
}
