import { request } from '../../../shared/api/http'
import type { Assessment, AssessmentInput, Page } from '../types/assessment'

export const assessmentsApi = {
  list: (page: number) => request<Page<Assessment>>(`/assessments?page=${page}&size=10`),
  get: (id: string) => request<Assessment>(`/assessments/${id}`),
  create: (input: AssessmentInput) => request<Assessment>('/assessments', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: string, input: AssessmentInput) => request<Assessment>(`/assessments/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  delete: (id: string) => request<void>(`/assessments/${id}`, { method: 'DELETE' }),
}
