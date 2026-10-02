import { useEffect, useState } from 'react'
import QRCode from 'qrcode'

export function QrCode({ value }: { value: string }) {
  const [src, setSrc] = useState('')
  useEffect(() => {
    void QRCode.toDataURL(value, { width: 240, margin: 1 }).then(setSrc)
  }, [value])
  if (!src) return <p>Preparing QR…</p>
  return <img src={src} alt={`QR code for ${value}`} width={240} height={240} />
}
