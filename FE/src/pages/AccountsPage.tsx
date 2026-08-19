import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAccounts, type Account } from '../api/accounts'
import { AppShell } from '../components/AppShell'
import { Icon } from '../components/Icon'

export function AccountsPage() {
  const navigate = useNavigate()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [totalBalance, setTotalBalance] = useState(0)
  const [balanceVisible, setBalanceVisible] = useState(true)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  const loadAccounts = useCallback(async (signal?: AbortSignal, refresh = false, initial = false) => {
    if (!initial) {
      if (refresh) setRefreshing(true)
      else setLoading(true)
      setError('')
    }
    try {
      const response = await getAccounts(signal)
      setAccounts(response.accounts)
      setTotalBalance(response.totalBalance)
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === 'AbortError') return
      setError(cause instanceof Error ? cause.message : '계좌 정보를 불러오지 못했습니다.')
    } finally {
      if (!signal?.aborted) {
        setLoading(false)
        setRefreshing(false)
      }
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void getAccounts(controller.signal)
      .then((response) => {
        setAccounts(response.accounts)
        setTotalBalance(response.totalBalance)
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof Error ? cause.message : '계좌 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [])

  const displayBalance = (value: number) => balanceVisible ? `${value.toLocaleString('ko-KR')}원` : '••••••••원'

  return <AppShell className="accounts-canvas" label="내 계좌 확인">
    <div className="accounts-sticky-header">
      <header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={() => navigate('/')}><span aria-hidden="true">‹</span> 이전</button><strong>내 계좌</strong><span /></header>
    </div>

    <section className="account-summary" aria-labelledby="total-balance-title" aria-busy={loading || refreshing}>
      <div className="summary-heading"><div><span>연결된 계좌 {accounts.length}개</span><h1 id="total-balance-title">내 계좌 총 잔액</h1></div><button type="button" className="balance-toggle" aria-pressed={!balanceVisible} onClick={() => setBalanceVisible((value) => !value)}><Icon name="eye" /><span>{balanceVisible ? '잔액 숨기기' : '잔액 보기'}</span></button></div>
      <strong className="total-balance">{loading ? '확인 중' : displayBalance(totalBalance)}</strong>
      <button type="button" className="refresh-accounts" onClick={() => void loadAccounts(undefined, true)} disabled={loading || refreshing}><Icon name="refresh" /><span>{refreshing ? '계좌 정보를 확인하고 있어요' : '새로고침'}</span></button>
    </section>

    <section className="account-list" aria-labelledby="account-list-title">
      <div className="account-list-heading"><h2 id="account-list-title">계좌별 잔액</h2><span>계좌를 누르면 자세히 볼 수 있어요</span></div>
      {error && <div className="account-state error" role="alert"><strong>계좌 정보를 불러오지 못했어요</strong><p>{error}</p><button type="button" onClick={() => void loadAccounts()}>다시 시도</button></div>}
      {!loading && !error && accounts.length === 0 && <div className="account-state"><strong>연결된 계좌가 없어요</strong><p>등록된 계좌를 확인해 주세요.</p></div>}
      {accounts.map((account, index) => <button type="button" className="account-card" key={account.accountNumber} onClick={() => navigate(`/accounts/${encodeURIComponent(account.accountNumber)}`)}>
        <span className={`bank-mark ${index % 2 === 0 ? 'navy' : 'gold'}`}>{account.bankName.slice(0, 1)}</span>
        <span className="account-info"><small>{account.bankName}</small><strong>{account.alias || `${account.bankName} 계좌`}</strong><span>{account.accountNumber}</span><b>{displayBalance(account.balance)}</b></span>
        <span className="account-chevron"><Icon name="chevron" /><small>상세</small></span>
      </button>)}
    </section>

    <aside className="account-help"><Icon name="shield" /><div><strong>모르는 계좌가 보이나요?</strong><p>직접 연결하지 않은 계좌라면 금융기관에 확인해 주세요.</p></div></aside>
  </AppShell>
}
