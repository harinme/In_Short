export type VoiceIntent = 'TRANSFER' | 'BALANCE' | 'HISTORY' | 'UNKNOWN'

export async function classifyVoiceIntent(transcript: string) {
  const response = await fetch('/api/ai/intent', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ transcript }),
  })
  if (!response.ok) throw new Error(`업무를 확인하지 못했습니다. (${response.status})`)
  return response.json() as Promise<{ intent: VoiceIntent }>
}
