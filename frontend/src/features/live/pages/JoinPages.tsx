import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { liveApi, storeParticipant } from '../api/live'
import '../../auth/pages/auth.css'

export function JoinPage() {
  const [code, setCode] = useState('')
  const navigate = useNavigate()
  function submit(event: FormEvent) {
    event.preventDefault()
    navigate(`/join/${code.trim().toUpperCase()}`)
  }
  return (
    <main className="authPage">
      <form className="authCard" onSubmit={submit}>
        <h1>AssessFlow</h1>
        <p>Enter session code</p>
        <label>
          Code
          <input
            required
            maxLength={6}
            value={code}
            onChange={(event) => setCode(event.target.value.toUpperCase())}
            placeholder="X7K92P"
            autoCapitalize="characters"
          />
        </label>
        <button type="submit">Continue</button>
      </form>
    </main>
  )
}

export function JoinCodePage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [title, setTitle] = useState('Session')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    liveApi
      .preview(code)
      .then((preview) => setTitle(preview.sessionName))
      .catch((cause) =>
        setError(cause instanceof Error ? cause.message : 'Could not load session.'),
      )
  }, [code])
  async function join(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const joined = await liveApi.join(code, name)
      storeParticipant(joined.sessionId, joined.participantToken, joined.participantId)
      navigate(`/play/${joined.sessionId}`)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not join.')
      setBusy(false)
    }
  }
  return (
    <main className="authPage">
      <form className="authCard" onSubmit={join}>
        <h1>{title}</h1>
        <p>Code {code}</p>
        <label>
          Your name
          <input
            required
            maxLength={200}
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        {error && <p role="alert">{error}</p>}
        <button disabled={busy} type="submit">
          Join session
        </button>
        <Link to="/join">Different code</Link>
      </form>
    </main>
  )
}
