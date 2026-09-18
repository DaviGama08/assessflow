import { API_BASE_URL } from '../../../config/env'
import { request } from '../../../shared/api/http'

export type LiveStatus = 'WAITING' | 'ACTIVE' | 'FINISHED' | 'CANCELLED'
export type LiveEventType =
  | 'PARTICIPANT_JOINED'
  | 'PARTICIPANT_LEFT'
  | 'SESSION_STARTED'
  | 'QUESTION_STARTED'
  | 'ANSWER_RECEIVED'
  | 'QUESTION_ENDED'
  | 'QUESTION_RESULTS'
  | 'SESSION_FINISHED'
  | 'SESSION_CANCELLED'

export type LiveSession = {
  id: string
  organizationId: string
  assessmentId: string
  assessmentTitle: string
  joinCode: string
  status: LiveStatus
  currentQuestionIndex: number | null
  questionOpen: boolean
  currentQuestionStartedAt: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
}

export type LiveParticipant = {
  id: string
  displayName: string
  status: string
  joinedAt: string
}

export type PublicQuestion = {
  questionId: string
  text: string
  type: string
  index: number
  total: number
  options: { id: string; text: string }[]
}

export type QuestionResults = {
  questionId: string
  text: string
  answered: number
  participants: number
  options: { id: string; text: string; correct: boolean; votes: number; percent: number }[]
}

export type ParticipantState = {
  sessionId: string
  sessionName: string
  status: LiveStatus
  questionOpen: boolean
  alreadyAnswered: boolean
  question: PublicQuestion | null
  results: QuestionResults | null
  score: { pointsEarned: number; pointsPossible: number; percentage: number } | null
  showResults: boolean
}

const host = (organizationId: string) => `/organizations/${organizationId}/live-sessions`

export const liveApi = {
  create: (organizationId: string, assessmentId: string) =>
    request<LiveSession>(
      `/organizations/${organizationId}/assessments/${assessmentId}/live-sessions`,
      {
        method: 'POST',
      },
    ),
  get: (organizationId: string, sessionId: string) =>
    request<LiveSession>(`${host(organizationId)}/${sessionId}`),
  participants: (organizationId: string, sessionId: string) =>
    request<LiveParticipant[]>(`${host(organizationId)}/${sessionId}/participants`),
  start: (organizationId: string, sessionId: string) =>
    request<LiveSession>(`${host(organizationId)}/${sessionId}/start`, { method: 'POST' }),
  endQuestion: (organizationId: string, sessionId: string) =>
    request<QuestionResults>(`${host(organizationId)}/${sessionId}/questions/end`, {
      method: 'POST',
    }),
  nextQuestion: (organizationId: string, sessionId: string) =>
    request<LiveSession>(`${host(organizationId)}/${sessionId}/questions/next`, { method: 'POST' }),
  finish: (organizationId: string, sessionId: string) =>
    request<LiveSession>(`${host(organizationId)}/${sessionId}/finish`, { method: 'POST' }),
  cancel: (organizationId: string, sessionId: string) =>
    request<LiveSession>(`${host(organizationId)}/${sessionId}/cancel`, { method: 'POST' }),
  currentQuestion: (organizationId: string, sessionId: string) =>
    request<PublicQuestion>(`${host(organizationId)}/${sessionId}/current-question`),
  results: (organizationId: string, sessionId: string) =>
    request<QuestionResults>(`${host(organizationId)}/${sessionId}/results`),
  preview: (code: string) =>
    guest<{ sessionName: string; status: LiveStatus; joinable: boolean }>(
      `/live-sessions/preview?code=${encodeURIComponent(code)}`,
    ),
  join: (code: string, displayName: string) =>
    guest<{
      participantId: string
      sessionId: string
      sessionName: string
      status: LiveStatus
      joinCode: string
      participantToken: string
    }>('/live-sessions/join', {
      method: 'POST',
      body: JSON.stringify({ code, displayName }),
    }),
  state: (sessionId: string, token: string) =>
    guest<ParticipantState>(`/live-sessions/${sessionId}/state`, undefined, token),
  answer: (sessionId: string, optionIds: string[], token: string) =>
    guest<void>(
      `/live-sessions/${sessionId}/answers`,
      {
        method: 'POST',
        body: JSON.stringify({ optionIds }),
      },
      token,
    ),
}

async function guest<T>(path: string, options?: RequestInit, token?: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as { detail?: string } | null
    throw new Error(problem?.detail ?? `Request failed (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return text ? (JSON.parse(text) as T) : (undefined as T)
}

export function storeParticipant(sessionId: string, token: string, participantId: string) {
  sessionStorage.setItem(`af-live-${sessionId}`, JSON.stringify({ token, participantId }))
}

export function loadParticipant(
  sessionId: string,
): { token: string; participantId: string } | null {
  const raw = sessionStorage.getItem(`af-live-${sessionId}`)
  return raw ? (JSON.parse(raw) as { token: string; participantId: string }) : null
}
