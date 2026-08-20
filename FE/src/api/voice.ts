import { csrfHeaders } from './auth'

export type VoiceConversation = {
  conversationId: string
  createdAt: string
  messages: Array<{ content: string; createdAt: string }>
  ttlSeconds: number
}

export type VoiceIntent = 'TRANSFER' | 'BALANCE' | 'HISTORY' | 'UNKNOWN'
export type InterpretationStatus = 'READY' | 'NEEDS_CLARIFICATION' | 'UNSUPPORTED'
export type NextAction = 'OPEN_TRANSFER' | 'OPEN_ACCOUNTS' | 'OPEN_HISTORY' | 'ASK_FOLLOW_UP' | 'RETRY'
export type MissingField = 'RECIPIENT' | 'AMOUNT' | 'ACCOUNT' | 'DATE_RANGE'

export type VoiceInterpretation = {
  conversationId: string
  requestId: string
  transcript: string
  intent: VoiceIntent
  status: InterpretationStatus
  nextAction: NextAction
  slots: {
    transfer: { recipientName: string | null; amount: number | null } | null
    balance: { accountHint: string | null } | null
    history: { accountHint: string | null; fromDate: string | null; toDate: string | null } | null
  }
  missingFields: MissingField[]
  message: string
}

export type TranscriptionResponse = {
  requestId: string
  transcript: string
  createdAt: string
  interpretation?: VoiceInterpretation
}

async function readError(response: Response) {
  const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
  return body?.detail || body?.message || `요청 처리에 실패했습니다. (${response.status})`
}

export async function createVoiceConversation() {
  const response = await fetch('/api/voice-conversations', { method: 'POST', headers: await csrfHeaders() })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<VoiceConversation>
}

export async function transcribeAudio(conversationId: string, audio: Blob) {
  const formData = new FormData()
  const extension = audio.type.includes('wav') ? 'wav' : audio.type.includes('mp4') ? 'm4a' : 'webm'
  formData.append('requestId', crypto.randomUUID())
  formData.append('audio', audio, `speech.${extension}`)
  const response = await fetch(`/api/voice-conversations/${conversationId}/transcriptions`, { method: 'POST', headers: await csrfHeaders(), body: formData })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<TranscriptionResponse>
}

export async function deleteVoiceConversation(conversationId: string) {
  const response = await fetch(`/api/voice-conversations/${conversationId}`, { method: 'DELETE', headers: await csrfHeaders() })
  if (!response.ok && response.status !== 404) throw new Error(await readError(response))
}
