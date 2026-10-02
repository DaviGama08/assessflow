import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useParams } from 'react-router-dom'
import {
  organizationsApi,
  type Branding,
  type Organization,
  type Role,
} from '../organizations/api/organizations'

type OrganizationValue = {
  organizationId: string
  organization: Organization | null
  branding: Branding | null
  role?: Role
  error: string
}

const OrganizationContext = createContext<OrganizationValue | null>(null)

export function OrganizationProvider({ children }: { children: ReactNode }) {
  const { organizationId = '' } = useParams()
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [branding, setBranding] = useState<Branding | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    setError('')
    setOrganization(null)
    Promise.all([organizationsApi.get(organizationId), organizationsApi.branding(organizationId)])
      .then(([nextOrganization, nextBranding]) => {
        setOrganization(nextOrganization)
        setBranding(nextBranding)
      })
      .catch((cause) =>
        setError(cause instanceof Error ? cause.message : 'Could not load workspace.'),
      )
  }, [organizationId])
  const value = useMemo(
    () => ({
      organizationId,
      organization,
      branding,
      role: organization?.currentUserRole,
      error,
    }),
    [organizationId, organization, branding, error],
  )
  return <OrganizationContext.Provider value={value}>{children}</OrganizationContext.Provider>
}

export function useOrganization() {
  const value = useContext(OrganizationContext)
  if (!value) throw new Error('OrganizationProvider is required')
  return value
}
