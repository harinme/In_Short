export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
export type TransactionType = 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER'
export type TransactionStatus = 'COMPLETED' | 'PENDING' | 'CANCELED'
export type TransactionChannel = 'MOBILE' | 'ATM' | 'AUTO_TRANSFER' | 'BANK_COUNTER' | 'OPEN_BANKING'

export type Account = {
  accountNumber: string
  bankName: string
  bankCode: string
  holder: string
  alias: string | null
  balance: number
  status: AccountStatus
}

export type AccountList = { totalBalance: number; accounts: Account[] }

export type Transaction = {
  transactionId: number
  transactedAt: string
  type: TransactionType
  status: TransactionStatus
  amount: number
  balanceAfter: number
  counterpartyBankName: string
  counterpartyBankCode: string
  counterpartyName: string
  counterpartyAccount: string
  memo: string | null
}

export type TransactionList = {
  accountNumber: string
  from: string
  to: string
  page: number
  size: number
  numberOfElements: number
  totalElements: number
  hasNext: boolean
  nextPage: number | null
  transactions: Transaction[]
}

export type TransactionDetail = Transaction & {
  accountNumber: string
  fee: number
  channel: TransactionChannel
  transferId: string | null
  referenceNumber: string
  canceledAt: string | null
  receiptAvailable: boolean
}

async function readError(response: Response) {
  const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
  return body?.detail || body?.message || `요청 처리에 실패했습니다. (${response.status})`
}

async function get<T>(url: string, signal?: AbortSignal) {
  const response = await fetch(url, { signal })
  if (!response.ok) throw new Error(await readError(response))
  return response.json() as Promise<T>
}

export function getAccounts(signal?: AbortSignal) {
  return get<AccountList>('/api/accounts', signal)
}

export function getAccount(accountNumber: string, signal?: AbortSignal) {
  return get<Account>(`/api/accounts/${encodeURIComponent(accountNumber)}`, signal)
}

export function getTransactions(accountNumber: string, page = 0, signal?: AbortSignal) {
  const params = new URLSearchParams({ page: String(page), size: '20' })
  return get<TransactionList>(`/api/accounts/${encodeURIComponent(accountNumber)}/transactions?${params}`, signal)
}

export function getTransaction(accountNumber: string, transactionId: number, signal?: AbortSignal) {
  return get<TransactionDetail>(`/api/accounts/${encodeURIComponent(accountNumber)}/transactions/${transactionId}`, signal)
}
