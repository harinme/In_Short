import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../components/AppShell'

type TransferStep = 'account' | 'amount' | 'fraudCheck' | 'confirm' | 'complete'

const banks = ['한마디은행', '국민은행', '신한은행', '우리은행', '하나은행']
const HIGH_VALUE_THRESHOLD = 1_000_000
const fraudQuestions = [
  { title: '기관에서 송금을 요청했나요?', description: '검찰, 경찰, 금융기관이 안전 계좌로 돈을 보내라고 했나요?' },
  { title: '새 번호로 연락받았나요?', description: '가족이나 지인이 새 번호로 연락해 급하게 돈을 요청했나요?' },
  { title: '먼저 돈을 보내라고 했나요?', description: '대출, 투자, 취업을 이유로 수수료나 보증금을 먼저 요구했나요?' },
]

export function NewTransferPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<TransferStep>('account')
  const [bank, setBank] = useState('')
  const [account, setAccount] = useState('')
  const [amount, setAmount] = useState('')
  const [error, setError] = useState('')
  const [fraudQuestionIndex, setFraudQuestionIndex] = useState(0)
  const [fraudDetected, setFraudDetected] = useState(false)

  const formattedAmount = Number(amount || 0).toLocaleString('ko-KR')
  const isHighValue = Number(amount) >= HIGH_VALUE_THRESHOLD
  const progressStep = step === 'account' ? 1 : step === 'amount' ? 2 : 3
  useEffect(() => {
    document.querySelector<HTMLElement>('.transfer-canvas')?.scrollTo({ top: 0 })
  }, [step, fraudDetected, fraudQuestionIndex])
  const keepInputVisible = (element: HTMLElement) => {
    window.setTimeout(() => element.scrollIntoView({ behavior: 'smooth', block: 'center' }), 180)
  }
  const goBack = () => {
    if (step === 'account') navigate('/')
    else if (step === 'amount') setStep('account')
    else if (step === 'fraudCheck') setStep('amount')
    else if (step === 'confirm') setStep(isHighValue ? 'fraudCheck' : 'amount')
    else navigate('/')
  }

  const verifyAccount = () => {
    if (!bank) return setError('은행을 선택해 주세요.')
    if (account.length < 8) return setError('계좌번호를 정확히 입력해 주세요.')
    setError('')
    setStep('amount')
  }

  const verifyAmount = () => {
    if (!amount || Number(amount) < 1) return setError('보낼 금액을 입력해 주세요.')
    setError('')
    setFraudQuestionIndex(0)
    setFraudDetected(false)
    setStep(Number(amount) >= HIGH_VALUE_THRESHOLD ? 'fraudCheck' : 'confirm')
  }

  const answerFraudQuestion = (riskDetected: boolean) => {
    if (riskDetected) {
      setFraudDetected(true)
      return
    }
    if (fraudQuestionIndex < fraudQuestions.length - 1) {
      setFraudQuestionIndex((index) => index + 1)
      return
    }
    setStep('confirm')
  }

  const visualState = step === 'fraudCheck' ? (fraudDetected ? 'danger-state' : 'warning-state') : step === 'complete' ? 'success-state' : ''

  return <AppShell className={`transfer-canvas ${visualState}`} label="새로운 사람에게 송금">
    <div className="transfer-sticky-header">
      <header className="flow-header"><button type="button" className="back-button" aria-label="이전 화면" onClick={goBack}><span aria-hidden="true">‹</span> 이전</button><strong>{step === 'complete' ? '송금 완료' : '새 계좌로 송금'}</strong><span /></header>
      {step !== 'complete' && <div className="step-progress" aria-label={`송금 ${progressStep}단계`}>{['계좌', '금액', '확인'].map((label, index) => <div key={label} className={progressStep >= index + 1 ? 'active' : ''} aria-current={progressStep === index + 1 ? 'step' : undefined}><span>{index + 1}</span><small>{label}</small></div>)}</div>}
    </div>

    {step === 'account' && <section className="transfer-step">
      <div className="step-copy"><span>1단계</span><h1>누구에게<br />보낼까요?</h1><p>처음 보내는 분의 은행과 계좌번호를 입력해 주세요.</p></div>
      <label className="field-label" htmlFor="bank">은행</label>
      <select id="bank" className="form-control" value={bank} onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => { setBank(event.target.value); setError('') }}><option value="">은행을 선택해 주세요</option>{banks.map((item) => <option key={item}>{item}</option>)}</select>
      <label className="field-label" htmlFor="account">계좌번호</label>
      <input id="account" className="form-control" type="text" inputMode="numeric" enterKeyHint="done" autoComplete="off" placeholder="숫자만 입력해 주세요" value={account} onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => { setAccount(event.target.value.replace(/\D/g, '').slice(0, 16)); setError('') }} />
      <p className="form-error" role="alert">{error || '\u00a0'}</p>
      <button type="button" className="flow-primary" onClick={verifyAccount}>받는 분 확인하기</button>
    </section>}

    {step === 'amount' && <section className="transfer-step">
      <div className="recipient-card"><span className="recipient-avatar">김</span><div><small>받는 분</small><strong>김민수</strong><p>{bank} · {account}</p></div><span className="verified-badge">확인됨</span></div>
      <div className="step-copy amount-copy"><span>2단계</span><h1>얼마를<br />보낼까요?</h1></div>
      <div className="amount-input"><input aria-label="송금 금액" type="text" inputMode="numeric" enterKeyHint="done" autoFocus value={amount ? formattedAmount : ''} placeholder="0" onFocus={(event) => keepInputVisible(event.currentTarget)} onChange={(event) => { setAmount(event.target.value.replace(/\D/g, '').slice(0, 9)); setError('') }} /><strong>원</strong></div>
      <div className="amount-shortcuts">{[10000, 50000, 100000].map((value) => <button key={value} type="button" onClick={() => setAmount(String(Number(amount || 0) + value))}>+{value.toLocaleString()}원</button>)}</div>
      <p className="balance-info">보낼 수 있는 금액 1,250,000원</p><p className="form-error" role="alert">{error || '\u00a0'}</p>
      <button type="button" className="flow-primary" onClick={verifyAmount}>다음</button>
    </section>}

    {step === 'fraudCheck' && <section className="transfer-step fraud-step">
      {!fraudDetected ? <>
        <div className="fraud-heading"><span className="warning-symbol">!</span><div><span>고액 송금 안전 확인</span><h1>사기 위험을<br />하나씩 확인할게요</h1></div></div>
        <p className="fraud-intro">처음 보내는 계좌에 {formattedAmount}원을 보내려고 해요.</p>
        <div className="warning-banner"><span aria-hidden="true">주의</span><strong>서두르지 말고 천천히 확인하세요</strong></div>
        <div className="fraud-question-count">질문 {fraudQuestionIndex + 1} / {fraudQuestions.length}</div>
        <div className="fraud-question" role="group" aria-labelledby="fraud-question-title"><span>확인 질문</span><h2 id="fraud-question-title">{fraudQuestions[fraudQuestionIndex].title}</h2><p>{fraudQuestions[fraudQuestionIndex].description}</p></div>
        <div className="fraud-answer-actions"><button type="button" className="risk-answer" onClick={() => answerFraudQuestion(true)}>예, 해당해요</button><button type="button" className="safe-answer" onClick={() => answerFraudQuestion(false)}>아니요</button></div>
      </> : <div className="fraud-detected" role="alert"><div className="danger-visual" aria-hidden="true"><span className="danger-ring" /><strong>!</strong></div><span className="danger-level">위험 신호가 발견됐어요</span><h2>지금은 송금을<br />멈추는 것이 안전해요</h2><p>상대방에게 다시 연락하지 말고 가족이나 금융기관의 공식 번호로 직접 확인해 주세요.</p><div className="danger-next-action"><strong>지금 해야 할 일</strong><span>송금을 중단하고 믿을 수 있는 사람에게 확인하세요.</span></div><button type="button" className="fraud-stop full-width" onClick={() => navigate('/')}>송금 멈추고 홈으로</button></div>}
    </section>}

    {step === 'confirm' && <section className="transfer-step confirm-step">
      <div className="step-copy"><span>3단계</span><h1>송금 내용을<br />확인해 주세요</h1><p>확인 버튼을 누르면 송금이 진행돼요.</p></div>
      <div className="transfer-summary"><div><span>받는 분</span><strong>김민수</strong></div><div><span>받는 계좌</span><strong>{bank}<br />{account}</strong></div><div className="summary-amount"><span>보낼 금액</span><strong>{formattedAmount}원</strong></div><div><span>수수료</span><strong>0원</strong></div></div>
      <button type="button" className="flow-primary" onClick={() => setStep('complete')}>{formattedAmount}원 보내기</button>
    </section>}

    {step === 'complete' && <section className="transfer-complete">
      <div className="complete-icon"><span className="success-ring" aria-hidden="true" /><strong aria-hidden="true">✓</strong></div><span>송금이 완료됐어요</span><h1>김민수님께<br />{formattedAmount}원을 보냈어요</h1><p>{bank} · {account}</p>
      <button type="button" className="flow-primary" onClick={() => navigate('/')}>홈으로 돌아가기</button>
    </section>}
  </AppShell>
}
