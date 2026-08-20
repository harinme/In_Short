export type Bank = { code: string; name: string }

export async function getBanks(signal?: AbortSignal) {
  const response = await fetch('/api/banks', { signal })
  if (!response.ok) throw new Error(`은행 목록을 불러오지 못했습니다. (${response.status})`)
  return response.json() as Promise<Bank[]>
}
