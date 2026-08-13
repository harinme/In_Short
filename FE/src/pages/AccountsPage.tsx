import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { Icon } from '../components/Icon'

const accounts = [
  { bank: '한마디은행', name: '생활비 통장', number: '123-456-789012', balance: 1_250_000, color: 'navy' },
  { bank: '국민은행', name: '연금 통장', number: '987-654-321098', balance: 3_480_500, color: 'gold' },
]

export function AccountsPage() {
  const navigate = useNavigate()
  const [balanceVisible, setBalanceVisible] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const totalBalance = accounts.reduce((sum, account) => sum + account.balance, 0)

  const refreshAccounts = () => {
    setRefreshing(true)
    window.setTimeout(() => setRefreshing(false), 700)
  }

  const displayBalance = (value: number) => balanceVisible ? `${value.toLocaleString('ko-KR')}원` : '••••••••원'

  return <AppShell className="accounts-canvas" label="내 계좌 확인">
    <div className="accounts-sticky-header">
      <header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={() => navigate('/')}><span aria-hidden="true">‹</span> 이전</button><strong>내 계좌</strong><span /></header>
    </div>

    <section className="account-summary" aria-labelledby="total-balance-title">
      <div className="summary-heading"><div><span>연결된 계좌 {accounts.length}개</span><h1 id="total-balance-title">내 계좌 총 잔액</h1></div><button type="button" className="balance-toggle" aria-pressed={!balanceVisible} onClick={() => setBalanceVisible((value) => !value)}><Icon name="eye" /><span>{balanceVisible ? '잔액 숨기기' : '잔액 보기'}</span></button></div>
      <strong className="total-balance">{displayBalance(totalBalance)}</strong>
      <button type="button" className="refresh-accounts" onClick={refreshAccounts} disabled={refreshing}><Icon name="refresh" /><span>{refreshing ? '계좌 정보를 확인하고 있어요' : '방금 업데이트됨 · 새로고침'}</span></button>
    </section>

    <section className="account-list" aria-labelledby="account-list-title">
      <div className="account-list-heading"><h2 id="account-list-title">계좌별 잔액</h2><span>계좌를 누르면 자세히 볼 수 있어요</span></div>
      {accounts.map((account) => <button type="button" className="account-card" key={account.number} onClick={() => navigate(`/accounts/${account.number.replaceAll('-', '')}`, { state: account })}>
        <span className={`bank-mark ${account.color}`}>{account.bank.slice(0, 1)}</span>
        <span className="account-info"><small>{account.bank}</small><strong>{account.name}</strong><span>{account.number}</span><b>{displayBalance(account.balance)}</b></span>
        <span className="account-chevron"><Icon name="chevron" /><small>상세</small></span>
      </button>)}
    </section>

    <aside className="account-help"><Icon name="shield" /><div><strong>모르는 계좌가 보이나요?</strong><p>직접 연결하지 않은 계좌라면 금융기관에 확인해 주세요.</p></div></aside>
  </AppShell>
}
