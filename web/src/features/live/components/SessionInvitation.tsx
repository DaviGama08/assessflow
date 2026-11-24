import type { LocalLiveStatus } from '../api/localLive'
import { QrCode } from './QrCode'
import { joinUrl } from '../joinUrl'

export function SessionInvitation({
  code,
  local,
}: {
  code: string
  local: LocalLiveStatus | null
}) {
  const url = local ? new URL(code, local.joinBaseUrl).href : joinUrl(code)
  const phoneCanReach = !['localhost', '127.0.0.1', '[::1]', '0.0.0.0'].includes(
    new URL(url).hostname,
  )
  return (
    <div className="liveStageTop" lang="pt">
      <div>
        <h2>Entrar na aula</h2>
        <p>
          {local
            ? 'Na mesma rede Wi-Fi do professor, leia o QR para abrir esta sessão.'
            : 'Leia o QR para abrir esta sessão no navegador.'}
        </p>
        <h3 className="joinCode">Code {code}</h3>
        {phoneCanReach ? (
          <a className="joinLink" href={url} target="_blank" rel="noreferrer">
            Abrir ligação da aula ↗
          </a>
        ) : (
          <p>
            Ligue o computador ao Wi-Fi da sala e abra a aplicação pelo endereço da rede para
            partilhar o QR.
          </p>
        )}
      </div>
      {phoneCanReach && (
        <div className="hostQr">
          <QrCode value={url} label="Entrar nesta aula" />
        </div>
      )}
    </div>
  )
}
