import { useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { WS_URL } from '../../config/env'
import type { LiveEventType } from './api/live'

export type LiveSocketEvent = { type: LiveEventType; payload: unknown }

export function useLiveEvents(
  sessionId: string | undefined,
  token: string | null,
  onEvent: (event: LiveSocketEvent) => void,
) {
  const [connection, setConnection] = useState('Connecting…')
  useEffect(() => {
    if (!sessionId || !token) return
    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 2000,
      onConnect: () => {
        setConnection('Connected')
        client.subscribe(`/topic/sessions/${sessionId}`, (message) => {
          onEvent(JSON.parse(message.body) as LiveSocketEvent)
        })
      },
      onWebSocketClose: () => setConnection('Reconnecting…'),
      onStompError: () => setConnection('Reconnecting…'),
    })
    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [sessionId, token, onEvent])
  return connection
}
