import { useEffect, useState, type ChangeEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { QrCode } from '../components/QrCode'
import { localLiveApi, type HotspotInfo, type LocalLiveStatus } from '../api/localLive'

export function LocalLivePage() {
  const { organizationId = '' } = useParams()
  const navigate = useNavigate()
  const [status, setStatus] = useState<LocalLiveStatus | null>(null)
  const [hotspot, setHotspot] = useState<HotspotInfo | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void localLiveApi.status().then(setStatus)
    void localLiveApi
      .hotspot()
      .then(setHotspot)
      .catch(() => undefined)
  }, [])

  async function importFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) return
    setBusy(true)
    setError('')
    try {
      const body = JSON.parse(await file.text()) as unknown
      const imported = await localLiveApi.importPackage(body)
      navigate(`/app/organizations/${imported.organizationId}/assessments/${imported.assessmentId}`)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not import the package.')
    } finally {
      setBusy(false)
    }
  }

  if (!status) {
    return (
      <section className="panel">
        <h1>Local Live Mode</h1>
        <p>
          This workspace is not running AssessFlow Local. Use same-origin Local Live to work without
          Internet.
        </p>
      </section>
    )
  }

  return (
    <>
      <div className="workspaceHeading">
        <div>
          <p className="eyebrow">LOCAL LIVE MODE</p>
          <h1>AssessFlow Local</h1>
          <p>Network: Local · Internet: Not required</p>
        </div>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <section className="panel">
        <p>
          Backend ✓ · Database {status.database} · LAN {status.selectedHost} · WebSocket{' '}
          {status.websocket}
        </p>
        <p>
          HTTP port {status.port}. Override with LOCAL_LIVE_HOST if more than one address is listed.
        </p>
        <p>Addresses: {status.lanAddresses.join(', ') || 'none discovered'}</p>
        <p>
          Hotspot: {status.hotspotStatus}. Captive portal: {status.captivePortal}.
        </p>
        <p>
          Automatic captive portal opening is controlled by Android/iOS/OS and cannot be guaranteed.
        </p>
        <label>
          Import Local Event Package
          <input
            disabled={busy}
            type="file"
            accept="application/json,.assessflow,.json"
            onChange={importFile}
          />
        </label>
        <div className="inlineActions" style={{ marginTop: '1rem' }}>
          <button disabled={busy} onClick={() => void localLiveApi.startHotspot().then(setHotspot)}>
            Start Local Hotspot
          </button>
          <button
            className="buttonSecondary"
            disabled={busy}
            onClick={() => void localLiveApi.stopHotspot().then(setHotspot)}
          >
            Stop hotspot
          </button>
        </div>
        {hotspot && <p>{hotspot.message}</p>}
        {hotspot?.wifiQr && (
          <div className="hostQr">
            <p>Wi-Fi QR {hotspot.ssid}</p>
            <QrCode value={hotspot.wifiQr} />
            <p>
              Scan to connect. AssessFlow should open automatically. If it does not, scan the Join
              QR on the session screen.
            </p>
          </div>
        )}
        <p>
          <a href={`/app/organizations/${organizationId}/assessments`}>Open assessments</a>
        </p>
      </section>
    </>
  )
}
