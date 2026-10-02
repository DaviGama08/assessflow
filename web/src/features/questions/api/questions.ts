import { request } from '../../../shared/api/http'
import type { Question, QuestionCategory, QuestionInput, QuestionPage } from '../types'

const questionsRoot = (organizationId: string) => `/organizations/${organizationId}/questions`
const categoriesRoot = (organizationId: string) =>
  `/organizations/${organizationId}/question-categories`

export type QuestionFilters = {
  page?: number
  size?: number
  search?: string
  type?: string
  difficulty?: string
  status?: string
  category?: string
}

export const questionsApi = {
  list: (organizationId: string, filters: QuestionFilters = {}) => {
    const params = new URLSearchParams()
    params.set('page', String(filters.page ?? 0))
    params.set('size', String(filters.size ?? 20))
    if (filters.search) params.set('search', filters.search)
    if (filters.type) params.set('type', filters.type)
    if (filters.difficulty) params.set('difficulty', filters.difficulty)
    if (filters.status) params.set('status', filters.status)
    if (filters.category) params.set('category', filters.category)
    return request<QuestionPage>(`${questionsRoot(organizationId)}?${params.toString()}`)
  },
  get: (organizationId: string, id: string) =>
    request<Question>(`${questionsRoot(organizationId)}/${id}`),
  create: (organizationId: string, input: QuestionInput) =>
    request<Question>(questionsRoot(organizationId), {
      method: 'POST',
      body: JSON.stringify(input),
    }),
  update: (organizationId: string, id: string, input: QuestionInput) =>
    request<Question>(`${questionsRoot(organizationId)}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(input),
    }),
  archive: (organizationId: string, id: string) =>
    request<void>(`${questionsRoot(organizationId)}/${id}`, { method: 'DELETE' }),
  categories: (organizationId: string) =>
    request<QuestionCategory[]>(categoriesRoot(organizationId)),
  createCategory: (organizationId: string, name: string, slug: string) =>
    request<QuestionCategory>(categoriesRoot(organizationId), {
      method: 'POST',
      body: JSON.stringify({ name, slug }),
    }),
  updateCategory: (organizationId: string, id: string, name: string, slug: string) =>
    request<QuestionCategory>(`${categoriesRoot(organizationId)}/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ name, slug }),
    }),
  deleteCategory: (organizationId: string, id: string) =>
    request<void>(`${categoriesRoot(organizationId)}/${id}`, { method: 'DELETE' }),
}
