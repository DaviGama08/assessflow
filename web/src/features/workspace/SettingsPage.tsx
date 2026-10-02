import { useEffect, useState, type FormEvent } from 'react'
import {
  organizationsApi,
  type Branding,
  type Organization,
} from '../organizations/api/organizations'

export function SettingsPage({ organizationId }: { organizationId: string }) {
  const [organization, setOrganization] = useState<Organization | null>(null)
  const [name, setName] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [logoUrl, setLogoUrl] = useState('')
  const [primaryColor, setPrimaryColor] = useState('#5369E8')
  const [secondaryColor, setSecondaryColor] = useState('#101E32')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    Promise.all([organizationsApi.get(organizationId), organizationsApi.branding(organizationId)])
      .then(([nextOrganization, branding]) => {
        setOrganization(nextOrganization)
        setName(nextOrganization.name)
        applyBranding(branding)
      })
      .catch((cause) =>
        setError(cause instanceof Error ? cause.message : 'Could not load settings.'),
      )
  }, [organizationId])

  function applyBranding(branding: Branding) {
    setDisplayName(branding.displayName)
    setLogoUrl(branding.logoUrl ?? '')
    setPrimaryColor(branding.primaryColor || '#5369E8')
    setSecondaryColor(branding.secondaryColor || '#101E32')
  }

  async function save(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const renamed = await organizationsApi.rename(organizationId, name)
      setOrganization(renamed)
      const branding = await organizationsApi.updateBranding(organizationId, {
        displayName,
        logoUrl: logoUrl || null,
        primaryColor,
        secondaryColor,
      })
      applyBranding(branding)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not save settings.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">WORKSPACE</p>
          <h1>Settings</h1>
          <p>Organization name and safe branding for this workspace.</p>
        </div>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <section className="panel">
        <h2>Preview</h2>
        <div className="preview" style={{ ['--af-primary' as string]: primaryColor }}>
          {logoUrl ? <img src={logoUrl} alt="" /> : <span className="previewMark">AF</span>}
          <div>
            <strong>AssessFlow</strong>
            <div>powered workspace: {displayName || organization?.name || 'Workspace'}</div>
          </div>
        </div>
      </section>
      <section className="panel" style={{ marginTop: '1rem' }}>
        <form className="formGrid" onSubmit={save}>
          <label>
            Organization name
            <input
              required
              maxLength={200}
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </label>
          <label>
            Branding display name
            <input
              required
              maxLength={200}
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
            />
          </label>
          <label>
            Logo URL
            <input
              type="url"
              maxLength={500}
              value={logoUrl}
              onChange={(event) => setLogoUrl(event.target.value)}
              placeholder="https://example.com/logo.png"
            />
          </label>
          <label>
            Primary color
            <span className="colorField">
              <input
                type="color"
                aria-label="Primary color picker"
                value={primaryColor}
                onChange={(event) => setPrimaryColor(event.target.value.toUpperCase())}
              />
              <input
                required
                pattern="#[0-9A-Fa-f]{6}"
                value={primaryColor}
                onChange={(event) => setPrimaryColor(event.target.value)}
              />
            </span>
          </label>
          <label>
            Secondary color
            <span className="colorField">
              <input
                type="color"
                aria-label="Secondary color picker"
                value={secondaryColor}
                onChange={(event) => setSecondaryColor(event.target.value.toUpperCase())}
              />
              <input
                required
                pattern="#[0-9A-Fa-f]{6}"
                value={secondaryColor}
                onChange={(event) => setSecondaryColor(event.target.value)}
              />
            </span>
          </label>
          <div>
            <button disabled={busy} type="submit">
              {busy ? 'Saving…' : 'Save settings'}
            </button>
          </div>
        </form>
      </section>
    </>
  )
}
