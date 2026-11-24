import { useEffect, useState } from 'react'
import QRCode from 'qrcode'

export function QrCode({
  value,
  label = 'Scan to join the session',
}: {
  value: string
  label?: string
}) {
  const [result, setResult] = useState<{ value: string; src: string }>()
  useEffect(() => {
    let current = true
    QRCode.toDataURL(value, { scale: 12, margin: 4 }).then(
      (src) => {
        if (current) setResult({ value, src })
      },
      () => {
        if (current) setResult({ value, src: '' })
      },
    )
    return () => {
      current = false
    }
  }, [value])
  if (result?.value !== value) return <p role="status">A preparar o QR…</p>
  if (!result.src) return <p role="alert">Não foi possível gerar o QR.</p>
  return <img src={result.src} alt={label} width={320} height={320} />
}
