export type AuthUser = { userId: number; name: string; expiresAt: string }
type CsrfResponse = { headerName: string; token: string }

async function readError(response: Response) {
  const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
  return body?.detail || body?.message || `요청 처리에 실패했습니다. (${response.status})`
}

export async function csrfHeaders() {
  const response = await fetch('/api/auth/csrf')
  if (!response.ok) throw new Error(await readError(response))
  const csrf = await response.json() as CsrfResponse
  return { [csrf.headerName]: csrf.token }
}

export async function checkPhone(phone: string) {
  const response = await fetch('/api/auth/phone/check', { method: 'POST', headers: { 'Content-Type': 'application/json', ...await csrfHeaders() }, body: JSON.stringify({ phone }) })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<{ exists: boolean }>
}

export async function login(phone: string, pin: string) {
  const response = await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json', ...await csrfHeaders() }, body: JSON.stringify({ phone, pin }) })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<AuthUser>
}

export async function logout() {
  const response = await fetch('/api/auth/logout', { method: 'POST', headers: await csrfHeaders() })
  if (!response.ok) throw new Error(await readError(response))
}

export async function getMe() {
  const response = await fetch('/api/auth/me')
  if (response.status === 401) return null
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<AuthUser>
}
