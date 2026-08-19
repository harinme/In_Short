import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getAccount, getTransactions, type Account, type Transaction } from '../api/accounts'
import { AppShell } from '../components/AppShell'

const accountStatusLabel = { ACTIVE: '정상', INACTIVE: '비활성', SUSPENDED: '이용 정지' } as const

function transactionDate(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date(value))
}

function transactionTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', { hour: 'numeric', minute: '2-digit' }).format(new Date(value))
}

export function AccountDetailPage() {
  const navigate = useNavigate()
  const { accountNumber = '' } = useParams()
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [nextPage, setNextPage] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')

  const loadDetail = useCallback(async (signal?: AbortSignal, initial = false) => {
    if (!initial) {
      setLoading(true)
      setError('')
    }
    try {
      const [accountResponse, transactionResponse] = await Promise.all([
        getAccount(accountNumber, signal),
        getTransactions(accountNumber, 0, signal),
      ])
      setAccount(accountResponse)
      setTransactions(transactionResponse.transactions)
      setNextPage(transactionResponse.nextPage)
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === 'AbortError') return
      setError(cause instanceof Error ? cause.message : '계좌 상세 정보를 불러오지 못했습니다.')
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [accountNumber])

  useEffect(() => {
    const controller = new AbortController()
    void Promise.all([
      getAccount(accountNumber, controller.signal),
      getTransactions(accountNumber, 0, controller.signal),
    ])
      .then(([accountResponse, transactionResponse]) => {
        setAccount(accountResponse)
        setTransactions(transactionResponse.transactions)
        setNextPage(transactionResponse.nextPage)
      })
      .catch((cause: unknown) => {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setError(cause instanceof Error ? cause.message : '계좌 상세 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })
    return () => controller.abort()
  }, [accountNumber])

  const loadMore = async () => {
    if (nextPage === null || loadingMore) return
    setLoadingMore(true)
    try {
      const response = await getTransactions(accountNumber, nextPage)
      setTransactions((current) => [...current, ...response.transactions])
      setNextPage(response.nextPage)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '거래 내역을 더 불러오지 못했습니다.')
    } finally {
      setLoadingMore(false)
    }
  }

  return <AppShell className="accounts-canvas" label={`${account?.alias || '계좌'} 상세`}>
    <div className="accounts-sticky-header"><header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={() => navigate('/accounts')}><span aria-hidden="true">‹</span> 이전</button><strong>계좌 상세</strong><span /></header></div>

    {loading && <div className="account-state" role="status"><strong>계좌 정보를 확인하고 있어요</strong><p>잠시만 기다려 주세요.</p></div>}
    {!loading && error && !account && <div className="account-state error" role="alert"><strong>계좌 상세를 불러오지 못했어요</strong><p>{error}</p><button type="button" onClick={() => void loadDetail()}>다시 시도</button></div>}

    {account && <>
      <section className="account-detail-hero"><span>{account.bankName}</span><h1>{account.alias || `${account.bankName} 계좌`}</h1><p>{account.accountNumber}</p><small>현재 잔액</small><strong>{account.balance.toLocaleString('ko-KR')}원</strong></section>
      <section className="account-detail-info"><h2>계좌 정보</h2><div><span>예금주</span><strong>{account.holder}</strong></div><div><span>계좌 상태</span><strong className={account.status === 'ACTIVE' ? 'status-normal' : ''}>{accountStatusLabel[account.status]}</strong></div></section>
      <section className="transaction-section" aria-labelledby="transaction-title">
        <div className="transaction-heading"><div><h2 id="transaction-title">최근 거래 내역</h2><span>최근 3개월 내역을 최신순으로 보여드려요</span></div></div>
        {error && <p className="transaction-error" role="alert">{error}</p>}
        {transactions.length === 0 && <div className="account-state"><strong>최근 거래 내역이 없어요</strong><p>조회 기간에 발생한 거래가 없습니다.</p></div>}
        {transactions.length > 0 && <div className="transaction-list">
          {transactions.map((transaction, index) => {
            const date = transactionDate(transaction.transactedAt)
            const showDate = index === 0 || transactionDate(transactions[index - 1].transactedAt) !== date
            const isDeposit = transaction.type === 'DEPOSIT'
            const displayText = transaction.memo || transaction.counterpartyName
            return <div key={transaction.transactionId} className="transaction-group">
              {showDate && <h3>{date}</h3>}
              <button type="button" className="transaction-item" aria-label={`${displayText}, ${isDeposit ? '입금' : '출금'} ${transaction.amount.toLocaleString('ko-KR')}원 상세 보기`} onClick={() => navigate(`/accounts/${encodeURIComponent(account.accountNumber)}/transactions/${transaction.transactionId}`)}>
                <span className={`transaction-mark ${isDeposit ? 'deposit' : 'withdrawal'}`}>{isDeposit ? '입금' : '출금'}</span>
                <span className="transaction-info"><strong>{displayText}</strong><small>{transactionTime(transaction.transactedAt)} · {transaction.memo ? transaction.counterpartyName : transaction.counterpartyBankName}</small></span>
                <span className="transaction-amount"><strong className={isDeposit ? 'deposit' : ''}>{isDeposit ? '+' : '-'}{transaction.amount.toLocaleString('ko-KR')}원</strong><small>잔액 {transaction.balanceAfter.toLocaleString('ko-KR')}원</small></span>
              </button>
            </div>
          })}
        </div>}
        {nextPage !== null && <button type="button" className="transaction-more" disabled={loadingMore} onClick={() => void loadMore()}>{loadingMore ? '불러오는 중' : '거래 내역 더보기'}</button>}
      </section>
      <button type="button" className="flow-primary account-transfer-button" onClick={() => navigate('/transfer/new')}>이 계좌에서 송금하기</button>
    </>}
  </AppShell>
}
