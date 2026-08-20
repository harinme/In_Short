import { csrfHeaders } from './auth'

export type RelationshipType = 'SON' | 'DAUGHTER' | 'MOTHER' | 'FATHER' | 'SPOUSE' | 'SIBLING' | 'OTHER'

export async function saveContact(input: {
  bankCode: string
  accountNumber: string
  alias: string
  favorite: boolean
  relationshipType: RelationshipType | null
}) {
  const response = await fetch('/api/contacts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...await csrfHeaders() },
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
    throw new Error(body?.detail || body?.message || `계좌를 저장하지 못했습니다. (${response.status})`)
  }
  return response.json()
}
