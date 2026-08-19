import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getTransaction, type TransactionDetail } from '../api/accounts'
import { AppShell } from '../components/AppShell'

const statusLabel = { COMPLETED: '완료', PENDING: '처리 중', CANCELED: '취소' } as const
const channelLabel = { MOBILE: '모바일뱅킹', ATM: 'ATM', AUTO_TRANSFER: '자동이체', BANK_COUNTER: '은행 창구', OPEN_BANKING: '오픈뱅킹' } as const

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'short', hour: 'numeric', minute: '2-digit', second: '2-digit',
  }).format(new Date(value))
}

export function TransactionDetailPage() {
  const navigate = useNavigate()
  const { accountNumber = '', transactionId = '' } = useParams()
  const numericTransactionId = Number(transactionId)
  const invalidTransactionId = !Number.isInteger(numericTransactionId) || numericTransactionId < 1
  const [transaction, setTransaction] = useState<TransactionDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    if (invalidTransactionId) return () => controller.abort()
    void getTransaction(accountNumber, numericTransactionId, controller.signal)
      .then(setTransaction)
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof Error ? cause.message : '거래 상세 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [accountNumber, invalidTransactionId, numericTransactionId])

  const isDeposit = transaction?.type === 'DEPOSIT'

  return <AppShell className="accounts-canvas" label="거래 상세">
    <div className="accounts-sticky-header"><header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={() => navigate(`/accounts/${encodeURIComponent(accountNumber)}`)}><span aria-hidden="true">‹</span> 이전</button><strong>거래 상세</strong><span /></header></div>
    {loading && !invalidTransactionId && <div className="account-state" role="status"><strong>거래 정보를 확인하고 있어요</strong><p>잠시만 기다려 주세요.</p></div>}
    {invalidTransactionId && <div className="account-state error" role="alert"><strong>올바르지 않은 거래번호예요</strong><p>거래 내역에서 다시 선택해 주세요.</p><button type="button" onClick={() => navigate(`/accounts/${encodeURIComponent(accountNumber)}`)}>계좌로 돌아가기</button></div>}
    {!loading && error && <div className="account-state error" role="alert"><strong>거래 정보를 불러오지 못했어요</strong><p>{error}</p><button type="button" onClick={() => navigate(`/accounts/${encodeURIComponent(accountNumber)}`)}>계좌로 돌아가기</button></div>}
    {transaction && <>
      <section className={`transaction-detail-hero ${isDeposit ? 'deposit' : ''}`}>
        <span>{isDeposit ? '입금' : '출금'} · {statusLabel[transaction.status]}</span>
        <h1>{transaction.memo || transaction.counterpartyName}</h1>
        <strong>{isDeposit ? '+' : '-'}{transaction.amount.toLocaleString('ko-KR')}원</strong>
        <p>{formatDateTime(transaction.transactedAt)}</p>
      </section>
      <section className="transaction-detail-info" aria-label="거래 상세 정보">
        <div><span>거래 후 잔액</span><strong>{transaction.balanceAfter.toLocaleString('ko-KR')}원</strong></div>
        <div><span>수수료</span><strong>{transaction.fee.toLocaleString('ko-KR')}원</strong></div>
        <div><span>거래 채널</span><strong>{channelLabel[transaction.channel]}</strong></div>
        <div><span>상대방</span><strong>{transaction.counterpartyName}</strong></div>
        <div><span>상대 은행</span><strong>{transaction.counterpartyBankName}</strong></div>
        <div><span>상대 계좌</span><strong>{transaction.counterpartyAccount}</strong></div>
        {transaction.memo && <div><span>메모</span><strong>{transaction.memo}</strong></div>}
        <div><span>내 계좌</span><strong>{transaction.accountNumber}</strong></div>
        <div><span>거래번호</span><strong className="transaction-reference">{transaction.referenceNumber}</strong></div>
        {transaction.canceledAt && <div><span>취소 시각</span><strong>{formatDateTime(transaction.canceledAt)}</strong></div>}
      </section>
    </>}
  </AppShell>
}
