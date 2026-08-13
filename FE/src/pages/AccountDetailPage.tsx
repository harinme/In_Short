import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AppShell } from '../components/AppShell'

type Account = { bank: string; name: string; number: string; balance: number }

const transactions = [
  { id: 1, date: '오늘', time: '오전 10:24', title: '김민수', description: '간편 송금', amount: -50_000, balance: 1_250_000 },
  { id: 2, date: '오늘', time: '오전 8:10', title: '국민연금', description: '연금 입금', amount: 680_000, balance: 1_300_000 },
  { id: 3, date: '8월 12일', time: '오후 4:32', title: '한마디마트', description: '체크카드 결제', amount: -32_500, balance: 620_000 },
  { id: 4, date: '8월 12일', time: '오전 9:15', title: '관리비', description: '자동이체', amount: -120_000, balance: 652_500 },
]

export function AccountDetailPage() {
  const navigate = useNavigate()
  const account = useLocation().state as Account | null
  if (!account) return <Navigate to="/accounts" replace />

  return <AppShell className="accounts-canvas" label={`${account.name} 계좌 상세`}>
    <div className="accounts-sticky-header"><header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={() => navigate('/accounts')}><span aria-hidden="true">‹</span> 이전</button><strong>계좌 상세</strong><span /></header></div>
    <section className="account-detail-hero"><span>{account.bank}</span><h1>{account.name}</h1><p>{account.number}</p><small>현재 잔액</small><strong>{account.balance.toLocaleString('ko-KR')}원</strong></section>
    <section className="account-detail-info"><h2>계좌 정보</h2><div><span>예금주</span><strong>김한마디</strong></div><div><span>계좌 상태</span><strong className="status-normal">정상</strong></div><div><span>오늘 보낼 수 있는 금액</span><strong>1,000,000원</strong></div></section>
    <section className="transaction-section" aria-labelledby="transaction-title">
      <div className="transaction-heading"><div><h2 id="transaction-title">최근 거래 내역</h2><span>최근 입금과 출금 내역이에요</span></div><button type="button">전체 내역</button></div>
      <div className="transaction-list">
        {transactions.map((transaction, index) => {
          const showDate = index === 0 || transactions[index - 1].date !== transaction.date
          const isDeposit = transaction.amount > 0
          return <div key={transaction.id} className="transaction-group">
            {showDate && <h3>{transaction.date}</h3>}
            <button type="button" className="transaction-item" aria-label={`${transaction.title}, ${isDeposit ? '입금' : '출금'} ${Math.abs(transaction.amount).toLocaleString('ko-KR')}원`}>
              <span className={`transaction-mark ${isDeposit ? 'deposit' : 'withdrawal'}`}>{isDeposit ? '입금' : '출금'}</span>
              <span className="transaction-info"><strong>{transaction.title}</strong><small>{transaction.time} · {transaction.description}</small></span>
              <span className="transaction-amount"><strong className={isDeposit ? 'deposit' : ''}>{isDeposit ? '+' : '-'}{Math.abs(transaction.amount).toLocaleString('ko-KR')}원</strong><small>잔액 {transaction.balance.toLocaleString('ko-KR')}원</small></span>
            </button>
          </div>
        })}
      </div>
    </section>
    <button type="button" className="flow-primary account-transfer-button" onClick={() => navigate('/transfer/new')}>이 계좌에서 송금하기</button>
  </AppShell>
}
