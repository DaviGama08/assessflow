import { useEffect, useState, type FormEvent } from 'react'
import type { User } from '../../auth/api/auth'
import { organizationsApi, type Member, type Organization, type Role } from '../api/organizations'
import { navigate } from '../../../shared/navigation'
import { canManageMembers } from '../../../shared/permissions'
import './organizations.css'

function errorText(cause: unknown) {
  return cause instanceof Error ? cause.message : 'The request failed.'
}

export function OrganizationSelectorPage() {
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    organizationsApi
      .list()
      .then(setOrganizations)
      .catch((cause) => setError(errorText(cause)))
      .finally(() => setLoading(false))
  }, [])

  async function create(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const organization = await organizationsApi.create(name, slug)
      navigate(`/app/organizations/${organization.id}`)
    } catch (cause) {
      setError(errorText(cause))
      setBusy(false)
    }
  }

  return (
    <main className="orgPage">
      <div className="orgContainer">
        <div className="orgHeading">
          <div>
            <p className="orgEyebrow">ASSESSFLOW</p>
            <h1>Organizations</h1>
            <p>Choose a workspace or create one to get started.</p>
          </div>
        </div>
        {error && (
          <p className="orgError" role="alert">
            {error}
          </p>
        )}
        {loading ? (
          <p>Loading organizations…</p>
        ) : organizations.length > 0 ? (
          <section className="orgGrid" aria-label="Your organizations">
            {organizations.map((organization) => (
              <a
                className="orgCard"
                href={`/app/organizations/${organization.id}`}
                key={organization.id}
                onClick={(event) => {
                  event.preventDefault()
                  navigate(`/app/organizations/${organization.id}`)
                }}
              >
                <strong>{organization.name}</strong>
                <span>{organization.slug}</span>
                <small>{organization.status}</small>
              </a>
            ))}
          </section>
        ) : !error ? (
          <p>You are not a member of an organization yet.</p>
        ) : null}
        <section className="orgPanel">
          <h2>Create an organization</h2>
          <p>You will become its owner.</p>
          <form className="orgForm" onSubmit={create}>
            <label>
              Name
              <input
                required
                maxLength={200}
                value={name}
                onChange={(event) => setName(event.target.value)}
              />
            </label>
            <label>
              Slug
              <input
                required
                maxLength={100}
                value={slug}
                onChange={(event) => setSlug(event.target.value)}
                placeholder="my-organization"
              />
            </label>
            <button type="submit" disabled={busy}>
              Create organization
            </button>
          </form>
        </section>
      </div>
    </main>
  )
}

export function MembersPage({ id, user }: { id: string; user: User }) {
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [members, setMembers] = useState<Member[]>([])
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<Role>('PARTICIPANT')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const currentRole = members.find((member) => member.userId === user.id)?.role
  const ownerCount = members.filter((member) => member.role === 'OWNER').length
  const manager = canManageMembers(currentRole)
  const roles: Role[] =
    currentRole === 'OWNER'
      ? ['OWNER', 'ADMIN', 'INSTRUCTOR', 'PARTICIPANT']
      : ['ADMIN', 'INSTRUCTOR', 'PARTICIPANT']

  useEffect(() => {
    Promise.all([organizationsApi.get(id), organizationsApi.members(id)])
      .then(([organizationResult, memberResult]) => {
        setOrganization(organizationResult)
        setMembers(memberResult)
      })
      .catch((cause) => setError(errorText(cause)))
      .finally(() => setLoading(false))
  }, [id])

  async function reload() {
    setMembers(await organizationsApi.members(id))
  }
  async function add(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await organizationsApi.addMember(id, email, role)
      setEmail('')
      await reload()
    } catch (cause) {
      setError(errorText(cause))
    } finally {
      setBusy(false)
    }
  }
  async function change(member: Member, nextRole: Role) {
    setBusy(true)
    setError('')
    try {
      await organizationsApi.changeRole(id, member.id, nextRole)
      await reload()
    } catch (cause) {
      setError(errorText(cause))
    } finally {
      setBusy(false)
    }
  }
  async function remove(member: Member) {
    if (!window.confirm(`Remove ${member.displayName} from this organization?`)) return
    setBusy(true)
    setError('')
    try {
      await organizationsApi.removeMember(id, member.id)
      await reload()
    } catch (cause) {
      setError(errorText(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">WORKSPACE</p>
          <h1>Members</h1>
          <p>{organization?.name}</p>
        </div>
      </div>
      {error && (
        <p className="orgError" role="alert">
          {error}
        </p>
      )}
      {loading ? (
        <p>Loading members…</p>
      ) : (
        <>
          <section className="orgPanel">
            <h2>People</h2>
            <div className="memberList">
              {members.map((member) => {
                const lastOwner = member.role === 'OWNER' && ownerCount === 1
                const canManage =
                  manager && !lastOwner && (currentRole === 'OWNER' || member.role !== 'OWNER')
                return (
                  <div className="memberRow" key={member.id}>
                    <div>
                      <strong>{member.displayName}</strong>
                      <small>{member.email}</small>
                    </div>
                    {canManage ? (
                      <>
                        <select
                          aria-label={`Role for ${member.email}`}
                          disabled={busy}
                          value={member.role}
                          onChange={(event) => void change(member, event.target.value as Role)}
                        >
                          {roles.map((choice) => (
                            <option key={choice} value={choice}>
                              {choice}
                            </option>
                          ))}
                        </select>
                        <button
                          className="buttonDanger"
                          disabled={busy}
                          onClick={() => void remove(member)}
                        >
                          Remove
                        </button>
                      </>
                    ) : (
                      <span>{lastOwner ? 'OWNER · last owner' : member.role}</span>
                    )}
                  </div>
                )
              })}
            </div>
          </section>
          {manager && (
            <section className="orgPanel">
              <h2>Add a member</h2>
              <p>Enter the email of an existing AssessFlow user.</p>
              <form className="orgForm" onSubmit={add}>
                <label>
                  Email
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                  />
                </label>
                <label>
                  Role
                  <select value={role} onChange={(event) => setRole(event.target.value as Role)}>
                    {roles.map((choice) => (
                      <option key={choice} value={choice}>
                        {choice}
                      </option>
                    ))}
                  </select>
                </label>
                <button type="submit" disabled={busy}>
                  Add member
                </button>
              </form>
            </section>
          )}
        </>
      )}
    </>
  )
}
