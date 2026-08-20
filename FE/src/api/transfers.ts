import { csrfHeaders } from './auth'

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type RiskSignal = 'LARGE_AMOUNT' | 'NEW_RECIPIENT' | 'DAILY_ACCUMULATION' | 'NEAR_DAILY_LIMIT'

export type Recipient = {
  bankCode: string
  bankName: string
  accountNumber: string
  holder: string
  relationshipEligible: boolean
}

export type RecipientSuggestion = Recipient & { alias: string | null; favorite: boolean; saved: boolean; relationshipType: import('./contacts').RelationshipType | null }

export type TransferResponse = {
  transferId: string
  result: 'COMPLETED' | 'REVIEW_REQUIRED'
  amount: number
  fee: number
  balanceAfter: number | null
  recipientName: string
  recipientBankName: string
  recipientAccountNumber: string
  riskLevel: RiskLevel
  riskSignals: RiskSignal[]
}

export type TransferInput = {
  sourceAccountNumber: string
  recipientBankCode: string
  recipientAccountNumber: string
  amount: number
  memo: string | null
  channel: 'MOBILE'
  requestId: string
  riskConfirmed: boolean
}

async function readError(response: Response) {
  const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
  return body?.detail || body?.message || `요청 처리에 실패했습니다. (${response.status})`
}

export async function getRecipient(bankCode: string, accountNumber: string) {
  const params = new URLSearchParams({ bankCode, accountNumber })
  const response = await fetch(`/api/transfers/recipient?${params}`)
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<Recipient>
}

export async function getRecipientSuggestions(signal?: AbortSignal) {
  const response = await fetch('/api/transfers/recipient-suggestions', { signal })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<RecipientSuggestion[]>
}

export async function createTransfer(input: TransferInput) {
  const response = await fetch('/api/transfers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...await csrfHeaders() },
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<TransferResponse>
}
