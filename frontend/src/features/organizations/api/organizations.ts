import { request } from '../../../shared/api/http'

export type Role = 'OWNER' | 'ADMIN' | 'INSTRUCTOR' | 'PARTICIPANT'
export type Organization = {
  id: string
  name: string
  slug: string
  status: 'ACTIVE' | 'SUSPENDED'
  currentUserRole: Role
  createdAt: string
  updatedAt: string
}
export type Member = {
  id: string
  organizationId: string
  userId: string
  email: string
  displayName: string
  role: Role
  status: 'ACTIVE' | 'DISABLED'
  joinedAt: string
}
export type Branding = {
  organizationId: string
  displayName: string
  logoUrl: string | null
  primaryColor: string | null
  secondaryColor: string | null
}
export type Dashboard = {
  assessmentCount: number
  questionCount: number
  memberCount: number
}

export const organizationsApi = {
  list: () => request<Organization[]>('/organizations'),
  get: (id: string) => request<Organization>(`/organizations/${id}`),
  create: (name: string, slug: string) =>
    request<Organization>('/organizations', {
      method: 'POST',
      body: JSON.stringify({ name, slug }),
    }),
  rename: (id: string, name: string) =>
    request<Organization>(`/organizations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ name }),
    }),
  members: (id: string) => request<Member[]>(`/organizations/${id}/members`),
  addMember: (id: string, email: string, role: Role) =>
    request<Member>(`/organizations/${id}/members`, {
      method: 'POST',
      body: JSON.stringify({ email, role }),
    }),
  changeRole: (id: string, memberId: string, role: Role) =>
    request<Member>(`/organizations/${id}/members/${memberId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    }),
  removeMember: (id: string, memberId: string) =>
    request<void>(`/organizations/${id}/members/${memberId}`, { method: 'DELETE' }),
  dashboard: (id: string) => request<Dashboard>(`/organizations/${id}/dashboard`),
  branding: (id: string) => request<Branding>(`/organizations/${id}/branding`),
  updateBranding: (id: string, branding: Omit<Branding, 'organizationId'>) =>
    request<Branding>(`/organizations/${id}/branding`, {
      method: 'PUT',
      body: JSON.stringify(branding),
    }),
}
