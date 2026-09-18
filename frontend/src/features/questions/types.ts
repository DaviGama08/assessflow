export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE'
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type QuestionStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

export type QuestionCategory = {
  id: string
  organizationId: string
  name: string
  slug: string
  createdAt: string
}

export type AnswerOption = {
  id?: string
  text: string
  correct: boolean
  displayOrder?: number
}

export type Question = {
  id: string
  organizationId: string
  text: string
  type: QuestionType
  difficulty: QuestionDifficulty
  status: QuestionStatus
  explanation: string | null
  category: QuestionCategory
  options: AnswerOption[]
  createdAt: string
  updatedAt: string
}

export type QuestionInput = {
  text: string
  type: QuestionType
  difficulty: QuestionDifficulty
  categoryId: string
  explanation: string
  status?: QuestionStatus
  options: AnswerOption[]
}

export type QuestionPage = {
  content: Question[]
  totalPages: number
  totalElements: number
  number: number
  last: boolean
  first: boolean
}
