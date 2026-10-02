export type AssessmentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type Assessment = {
  id: string
  organizationId: string
  title: string
  description: string | null
  status: AssessmentStatus
  timeLimitMinutes: number | null
  maxAttempts: number
  passingScore: number | null
  shuffleQuestions: boolean
  shuffleAnswers: boolean
  showResultsAfterCompletion: boolean
  createdAt: string
  updatedAt: string
}
export type AssessmentInput = {
  title: string
  description: string
  timeLimitMinutes: number | null
  maxAttempts: number
  passingScore: number | null
  shuffleQuestions: boolean
  shuffleAnswers: boolean
  showResultsAfterCompletion: boolean
}
export type AssessmentQuestion = {
  id: string
  assessmentId: string
  questionId: string
  questionText: string
  type: string
  difficulty: string
  status: string
  points: number
  displayOrder: number
}
export type Page<T> = {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  last: boolean
  first: boolean
}
