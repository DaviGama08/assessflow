export type AssessmentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type Assessment = {
  id: string
  title: string
  description: string | null
  status: AssessmentStatus
  createdAt: string
  updatedAt: string
}
export type AssessmentInput = { title: string; description: string }
export type Page<T> = { content: T[]; totalPages: number; totalElements: number; number: number; last: boolean; first: boolean }
