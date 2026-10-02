import type { Role } from '../features/organizations/api/organizations'

export function canManageContent(role?: Role) {
  return role === 'OWNER' || role === 'ADMIN' || role === 'INSTRUCTOR'
}

export function canManageWorkspace(role?: Role) {
  return role === 'OWNER' || role === 'ADMIN'
}

export function canManageMembers(role?: Role) {
  return canManageWorkspace(role)
}
