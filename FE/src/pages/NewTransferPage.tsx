import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAccounts, type Account } from '../api/accounts'
import { getBanks, type Bank } from '../api/banks'
import { saveContact, type RelationshipType } from '../api/contacts'
import { createTransfer, getRecipient, getRecipientSuggestions, type Recipient, type RecipientSuggestion, type TransferResponse } from '../api/transfers'
import { AppShell } from '../components/AppShell'

type TransferStep = 'account' | 'amount' | 'fraudCheck' | 'confirm' | 'complete'
const HIGH_VALUE_THRESHOLD = 1_000_000
const fraudQuestions = [
  { title: '기관에서 송금을 요청했나요?', description: '검찰, 경찰, 금융기관이 안전 계좌로 돈을 보내라고 했나요?' },
  { title: '새 번호로 연락받았나요?', description: '가족이나 지인이 새 번호로 연락해 급하게 돈을 요청했나요?' },
  { title: '먼저 돈을 보내라고 했나요?', description: '대출, 투자, 취업을 이유로 수수료나 보증금을 먼저 요구했나요?' },
]
const relationshipLabels: Record<RelationshipType, string> = { SON: '아들', DAUGHTER: '딸', MOTHER: '어머니', FATHER: '아버지', SPOUSE: '배우자', SIBLING: '형제·자매', OTHER: '가족·지인' }

export function NewTransferPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<TransferStep>('account')
  const [accounts, setAccounts] = useState<Account[]>([])
  const [banks, setBanks] = useState<Bank[]>([])
  const [suggestions, setSuggestions] = useState<RecipientSuggestion[]>([])
  const [sourceAccountNumber, setSourceAccountNumber] = useState('')
  const [bankCode, setBankCode] = useState('')
  const [account, setAccount] = useState('')
  const [recipient, setRecipient] = useState<Recipient | null>(null)
  const [amount, setAmount] = useState('')
  const [memo, setMemo] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [fraudQuestionIndex, setFraudQuestionIndex] = useState(0)
  const [fraudDetected, setFraudDetected] = useState(false)
  const [riskReview, setRiskReview] = useState<TransferResponse | null>(null)
  const [completedTransfer, setCompletedTransfer] = useState<TransferResponse | null>(null)
  const [saveAlias, setSaveAlias] = useState('')
  const [saveFavorite, setSaveFavorite] = useState(false)
  const [relationshipType, setRelationshipType] = useState<RelationshipType | ''>('')
  const [savingContact, setSavingContact] = useState(false)
  const [contactSaved, setContactSaved] = useState(false)
  const [requestId, setRequestId] = useState(() => crypto.randomUUID())

  const sourceAccount = useMemo(() => accounts.find((item) => item.accountNumber === sourceAccountNumber) ?? null, [accounts, sourceAccountNumber])
  const formattedAmount = Number(amount || 0).toLocaleString('ko-KR')
  const isHighValue = Number(amount) >= HIGH_VALUE_THRESHOLD
  const progressStep = step === 'account' ? 1 : step === 'amount' ? 2 : 3

  useEffect(() => {
    const controller = new AbortController()
    void Promise.all([getAccounts(controller.signal), getBanks(controller.signal), getRecipientSuggestions(controller.signal)]).then(([accountResponse, bankResponse, suggestionResponse]) => {
      const active = accountResponse.accounts.filter((item) => item.status === 'ACTIVE')
      setAccounts(active)
      setBanks(bankResponse)
      setSuggestions(suggestionResponse)
      setSourceAccountNumber(active[0]?.accountNumber ?? '')
    }).catch((cause: unknown) => {
      if (cause instanceof DOMException && cause.name === 'AbortError') return
      setError(cause instanceof Error ? cause.message : '계좌를 불러오지 못했습니다.')
    }).finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [])

  useEffect(() => { document.querySelector<HTMLElement>('.transfer-canvas')?.scrollTo({ top: 0 }) }, [step, fraudDetected, fraudQuestionIndex])
  const keepInputVisible = (element: HTMLElement) => { window.setTimeout(() => element.scrollIntoView({ behavior: 'smooth', block: 'center' }), 180) }
  const goBack = () => {
    if (step === 'account') navigate('/')
    else if (step === 'amount') setStep('account')
    else if (step === 'fraudCheck') setStep('amount')
    else if (step === 'confirm') setStep(isHighValue ? 'fraudCheck' : 'amount')
    else navigate('/')
  }

  const verifyAccount = async () => {
    if (!sourceAccount) return setError('보낼 계좌를 선택해 주세요.')
    if (!bankCode) return setError('은행을 선택해 주세요.')
    if (account.length < 8) return setError('계좌번호를 정확히 입력해 주세요.')
    setLoading(true); setError('')
    try {
      const verified = await getRecipient(bankCode, account)
      if (verified.accountNumber === sourceAccount.accountNumber) return setError('같은 계좌로 송금할 수 없습니다.')
      setContactSaved(suggestions.some((item) => item.saved && item.bankCode === verified.bankCode && item.accountNumber.replace(/\D/g, '') === verified.accountNumber.replace(/\D/g, '')))
      setRecipient(verified); setStep('amount')
    } catch (cause) {
      setRecipient(null); setError(cause instanceof Error ? cause.message : '받는 계좌를 확인하지 못했습니다.')
    } finally { setLoading(false) }
  }

  const selectSuggestion = async (suggestion: RecipientSuggestion) => {
    setBankCode(suggestion.bankCode); setAccount(suggestion.accountNumber.replace(/\D/g, '')); setLoading(true); setError('')
    setContactSaved(suggestion.saved)
    try {
      const verified = await getRecipient(suggestion.bankCode, suggestion.accountNumber)
      if (verified.accountNumber === sourceAccount?.accountNumber) return setError('같은 계좌로 송금할 수 없습니다.')
      setRecipient(verified); setStep('amount')
    } catch (cause) { setError(cause instanceof Error ? cause.message : '받는 계좌를 확인하지 못했습니다.') }
    finally { setLoading(false) }
  }

  const verifyAmount = () => {
    const value = Number(amount)
    if (!value || value < 1) return setError('보낼 금액을 입력해 주세요.')
    if (!sourceAccount || value > sourceAccount.balance) return setError('보낼 수 있는 금액을 초과했습니다.')
    setError(''); setFraudQuestionIndex(0); setFraudDetected(false)
    setStep(value >= HIGH_VALUE_THRESHOLD ? 'fraudCheck' : 'confirm')
  }

  const answerFraudQuestion = (detected: boolean) => {
    if (detected) return setFraudDetected(true)
    if (fraudQuestionIndex < fraudQuestions.length - 1) return setFraudQuestionIndex((index) => index + 1)
    setStep('confirm')
  }

  const submitTransfer = async () => {
    if (!sourceAccount || !recipient) return setError('송금 정보를 다시 확인해 주세요.')
    setSubmitting(true); setError('')
    try {
      const response = await createTransfer({ sourceAccountNumber: sourceAccount.accountNumber, recipientBankCode: recipient.bankCode, recipientAccountNumber: recipient.accountNumber, amount: Number(amount), memo: memo.trim() || null, channel: 'MOBILE', requestId, riskConfirmed: riskReview !== null })
      if (response.result === 'REVIEW_REQUIRED') {
        setRiskReview(response)
        setError('이상거래 신호가 감지됐습니다. 내용을 다시 확인한 뒤 한 번 더 눌러 주세요.')
        return
      }
      setCompletedTransfer(response); setSaveAlias(response.recipientName); setStep('complete'); setRequestId(crypto.randomUUID())
    } catch (cause) { setError(cause instanceof Error ? cause.message : '송금을 처리하지 못했습니다.') }
    finally { setSubmitting(false) }
  }

  const saveRecipient = async () => {
    if (!recipient || !saveAlias.trim()) return setError('저장할 이름을 입력해 주세요.')
    setSavingContact(true); setError('')
    try {
      await saveContact({ bankCode: recipient.bankCode, accountNumber: recipient.accountNumber, alias: saveAlias.trim(), favorite: saveFavorite, relationshipType: relationshipType || null })
      setContactSaved(true)
    } catch (cause) { setError(cause instanceof Error ? cause.message : '계좌를 저장하지 못했습니다.') }
    finally { setSavingContact(false) }
  }

  const visualState = step === 'fraudCheck' ? (fraudDetected ? 'danger-state' : 'warning-state') : step === 'complete' ? 'success-state' : ''
  return <AppShell className={`transfer-canvas ${visualState}`} label="새로운 사람에게 송금">
    <div className="transfer-sticky-header"><header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={goBack}><span aria-hidden="true">‹</span> 이전</button><strong>{step === 'complete' ? '송금 완료' : '새 계좌로 송금'}</strong><span /></header>{step !== 'complete' && <div className="step-progress" aria-label={`송금 ${progressStep}단계`}>{['계좌', '금액', '확인'].map((label, index) => <div key={label} className={progressStep >= index + 1 ? 'active' : ''} aria-current={progressStep === index + 1 ? 'step' : undefined}><span>{index + 1}</span><small>{label}</small></div>)}</div>}</div>

    {step === 'account' && <section className="transfer-step"><div className="step-copy"><span>1단계</span><h1>누구에게<br />보낼까요?</h1><p>보낼 계좌와 받는 분의 계좌를 확인해 주세요.</p></div><label className="field-label" htmlFor="source-account">보낼 계좌</label><select id="source-account" className="form-control" value={sourceAccountNumber} disabled={loading} onChange={(event) => { setSourceAccountNumber(event.target.value); setError('') }}>{accounts.map((item) => <option key={item.accountNumber} value={item.accountNumber}>{item.bankName} {item.accountNumber} · {item.balance.toLocaleString()}원</option>)}</select>{suggestions.length > 0 && <div className="recipient-suggestions"><div className="suggestion-heading"><strong>자주 보내는 분</strong><span>즐겨찾기와 최근 송금</span></div><div className="suggestion-list">{suggestions.map((item) => <button type="button" key={`${item.bankCode}-${item.accountNumber}`} onClick={() => void selectSuggestion(item)}><span className="suggestion-copy"><small className="suggestion-kind">{item.relationshipType ? relationshipLabels[item.relationshipType] : item.favorite ? '즐겨찾기' : '최근 송금'}</small><strong>{item.alias || item.holder}{item.favorite && <small>★</small>}</strong><span>{item.alias && `${item.holder} · `}{item.bankName} · {item.accountNumber}</span></span></button>)}</div></div>}<label className="field-label" htmlFor="bank">받는 은행</label><select id="bank" className="form-control" value={bankCode} onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => { setBankCode(event.target.value); setRecipient(null); setError('') }}><option value="">은행을 선택해 주세요</option>{banks.map((item) => <option key={item.code} value={item.code}>{item.name}</option>)}</select><label className="field-label" htmlFor="account">받는 계좌번호</label><input id="account" className="form-control" type="text" inputMode="numeric" enterKeyHint="done" autoComplete="off" placeholder="숫자만 입력해 주세요" value={account} onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => { setAccount(event.target.value.replace(/\D/g, '').slice(0, 30)); setRecipient(null); setError('') }} /><p className="form-error" role="alert">{error || '\u00a0'}</p><button type="button" className="flow-primary" disabled={loading} onClick={() => void verifyAccount()}>{loading ? '계좌 확인 중' : '받는 분 확인하기'}</button></section>}

    {step === 'amount' && recipient && <section className="transfer-step"><div className="recipient-card"><span className="recipient-avatar">{recipient.holder.slice(0, 1)}</span><div><small>받는 분</small><strong>{recipient.holder}</strong><p>{recipient.bankName} · {recipient.accountNumber}</p></div><span className="verified-badge">확인됨</span></div><div className="step-copy amount-copy"><span>2단계</span><h1>얼마를<br />보낼까요?</h1></div><div className="amount-input"><input aria-label="송금 금액" type="text" inputMode="numeric" enterKeyHint="done" autoFocus value={amount ? formattedAmount : ''} placeholder="0" onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => { setAmount(event.target.value.replace(/\D/g, '').slice(0, 9)); setRiskReview(null); setError('') }} /><strong>원</strong></div><div className="amount-shortcuts">{[10000, 50000, 100000].map((value) => <button key={value} type="button" onClick={() => setAmount(String(Number(amount || 0) + value))}>+{value.toLocaleString()}원</button>)}</div><p className="balance-info">보낼 수 있는 금액 {sourceAccount?.balance.toLocaleString() ?? 0}원</p><label className="field-label" htmlFor="transfer-memo">메모 <small>(선택)</small></label><input id="transfer-memo" className="form-control" type="text" maxLength={100} placeholder="예: 생활비, 회비" value={memo} onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => setMemo(event.target.value)} /><p className="form-error" role="alert">{error || '\u00a0'}</p><button type="button" className="flow-primary" onClick={verifyAmount}>다음</button></section>}

    {step === 'fraudCheck' && <section className="transfer-step fraud-step">{!fraudDetected ? <><div className="fraud-heading"><span className="warning-symbol">!</span><div><span>고액 송금 안전 확인</span><h1>사기 위험을<br />하나씩 확인할게요</h1></div></div><p className="fraud-intro">처음 보내는 계좌에 {formattedAmount}원을 보내려고 해요.</p><div className="warning-banner"><span aria-hidden="true">주의</span><strong>서두르지 말고 천천히 확인하세요</strong></div><div className="fraud-question-count">질문 {fraudQuestionIndex + 1} / {fraudQuestions.length}</div><div className="fraud-question" role="group" aria-labelledby="fraud-question-title"><span>확인 질문</span><h2 id="fraud-question-title">{fraudQuestions[fraudQuestionIndex].title}</h2><p>{fraudQuestions[fraudQuestionIndex].description}</p></div><div className="fraud-answer-actions"><button type="button" className="risk-answer" onClick={() => answerFraudQuestion(true)}>예, 해당해요</button><button type="button" className="safe-answer" onClick={() => answerFraudQuestion(false)}>아니요</button></div></> : <div className="fraud-detected" role="alert"><div className="danger-visual" aria-hidden="true"><span className="danger-ring" /><strong>!</strong></div><span className="danger-level">위험 신호가 발견됐어요</span><h2>지금은 송금을<br />멈추는 것이 안전해요</h2><p>상대방에게 다시 연락하지 말고 가족이나 금융기관의 공식 번호로 직접 확인해 주세요.</p><button type="button" className="fraud-stop full-width" onClick={() => navigate('/')}>송금 멈추고 홈으로</button></div>}</section>}

    {step === 'confirm' && recipient && <section className="transfer-step confirm-step"><div className="step-copy"><span>3단계</span><h1>송금 내용을<br />확인해 주세요</h1><p>{riskReview ? '이상거래 신호가 감지됐습니다. 본인이 요청한 송금인지 다시 확인하세요.' : '확인 버튼을 누르면 송금이 진행돼요.'}</p></div><div className="transfer-summary"><div><span>보낼 계좌</span><strong>{sourceAccount?.bankName}<br />{sourceAccount?.accountNumber}</strong></div><div><span>받는 분</span><strong>{recipient.holder}</strong></div><div><span>받는 계좌</span><strong>{recipient.bankName}<br />{recipient.accountNumber}</strong></div><div className="summary-amount"><span>보낼 금액</span><strong>{formattedAmount}원</strong></div>{memo.trim() && <div><span>메모</span><strong>{memo.trim()}</strong></div>}<div><span>수수료</span><strong>0원</strong></div></div><p className="form-error" role="alert">{error || '\u00a0'}</p><button type="button" className="flow-primary" disabled={submitting} onClick={() => void submitTransfer()}>{submitting ? '송금 처리 중' : riskReview ? '위험 확인 후 송금하기' : `${formattedAmount}원 보내기`}</button></section>}

    {step === 'complete' && completedTransfer && recipient && <section className="transfer-complete"><div className="complete-icon"><span className="success-ring" aria-hidden="true" /><strong aria-hidden="true">✓</strong></div><span>송금이 완료됐어요</span><h1>{completedTransfer.recipientName}님께<br />{completedTransfer.amount.toLocaleString('ko-KR')}원을 보냈어요</h1><p>{completedTransfer.recipientBankName} · {completedTransfer.recipientAccountNumber}</p>{!contactSaved ? <div className="save-recipient-card"><strong>다음에도 쉽게 보내시겠어요?</strong><label htmlFor="save-alias">저장할 이름</label><input id="save-alias" className="form-control" maxLength={30} value={saveAlias} onChange={(event) => setSaveAlias(event.target.value)} /><label className="favorite-check"><input type="checkbox" checked={saveFavorite} onChange={(event) => setSaveFavorite(event.target.checked)} /> 즐겨찾기에 추가</label>{recipient.relationshipEligible && <><label htmlFor="relationship-type">나와의 관계 <small>(선택)</small></label><select id="relationship-type" className="form-control" value={relationshipType} onChange={(event) => setRelationshipType(event.target.value as RelationshipType | '')}><option value="">관계 설정 안 함</option><option value="SON">아들</option><option value="DAUGHTER">딸</option><option value="MOTHER">어머니</option><option value="FATHER">아버지</option><option value="SPOUSE">배우자</option><option value="SIBLING">형제·자매</option><option value="OTHER">기타</option></select></>}<p className="form-error" role="alert">{error || '\u00a0'}</p><button type="button" className="save-recipient-button" disabled={savingContact} onClick={() => void saveRecipient()}>{savingContact ? '저장 중' : '이 계좌 저장하기'}</button></div> : <div className="saved-recipient-message" role="status">이 계좌를 저장했어요{saveFavorite ? ' · 즐겨찾기 추가됨' : ''}</div>}<button type="button" className="flow-primary" onClick={() => navigate('/')}>홈으로 돌아가기</button></section>}
  </AppShell>
}
