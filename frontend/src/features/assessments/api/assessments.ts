import { request } from '../../../shared/api/http'
import type { Assessment, AssessmentInput, AssessmentQuestion, Page } from '../types/assessment'

const root = (organizationId: string) => `/organizations/${organizationId}/assessments`

export const assessmentsApi = {
  list: (organizationId: string, page: number) =>
    request<Page<Assessment>>(`${root(organizationId)}?page=${page}&size=10`),
  get: (organizationId: string, id: string) => request<Assessment>(`${root(organizationId)}/${id}`),
  create: (organizationId: string, input: AssessmentInput) =>
    request<Assessment>(root(organizationId), { method: 'POST', body: JSON.stringify(input) }),
  update: (organizationId: string, id: string, input: AssessmentInput) =>
    request<Assessment>(`${root(organizationId)}/${id}`, {
      method: 'PUT',
      body: JSON.stringify(input),
    }),
  delete: (organizationId: string, id: string) =>
    request<void>(`${root(organizationId)}/${id}`, { method: 'DELETE' }),
  questions: (organizationId: string, assessmentId: string) =>
    request<AssessmentQuestion[]>(`${root(organizationId)}/${assessmentId}/questions`),
  addQuestion: (
    organizationId: string,
    assessmentId: string,
    questionId: string,
    points: number,
    displayOrder?: number,
  ) =>
    request<AssessmentQuestion>(`${root(organizationId)}/${assessmentId}/questions/${questionId}`, {
      method: 'POST',
      body: JSON.stringify({ points, displayOrder }),
    }),
  updateQuestionPoints: (
    organizationId: string,
    assessmentId: string,
    questionId: string,
    points: number,
  ) =>
    request<AssessmentQuestion>(`${root(organizationId)}/${assessmentId}/questions/${questionId}`, {
      method: 'PATCH',
      body: JSON.stringify({ points }),
    }),
  removeQuestion: (organizationId: string, assessmentId: string, questionId: string) =>
    request<void>(`${root(organizationId)}/${assessmentId}/questions/${questionId}`, {
      method: 'DELETE',
    }),
  reorderQuestions: (organizationId: string, assessmentId: string, questionIds: string[]) =>
    request<AssessmentQuestion[]>(`${root(organizationId)}/${assessmentId}/questions/order`, {
      method: 'PUT',
      body: JSON.stringify({ questionIds }),
    }),
  publish: (organizationId: string, id: string) =>
    request<Assessment>(`${root(organizationId)}/${id}/publish`, { method: 'POST' }),
  archive: (organizationId: string, id: string) =>
    request<Assessment>(`${root(organizationId)}/${id}/archive`, { method: 'POST' }),
}
