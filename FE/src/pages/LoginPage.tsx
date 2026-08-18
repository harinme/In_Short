import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { checkPhone } from '../api/auth'
import { useAuth } from '../auth-context'
import { AppShell } from '../components/AppShell'
import { CharacterImage } from '../components/CharacterImage'
import { Icon } from '../components/Icon'
import { toast } from '../stores/toast'
import mainCharacter from '../assets/main-character.png'

type LoginStep = 'phone' | 'pin'
const PHONE_LENGTH = 11
const PHONE_PREFIX = '010'
const PIN_LENGTH = 6

function formatPhone(phone: string) {
  if (phone.length <= 3) return phone
  if (phone.length <= 7) return `${phone.slice(0, 3)}-${phone.slice(3)}`
  return `${phone.slice(0, 3)}-
  ${phone.slice(3, 7)}-${phone.slice(7)}`
}

function PhoneDisplay({ phone }: { phone: string }) {
  const entered = phone.slice(PHONE_PREFIX.length)
  const middle = Array.from({ length: 4 }, (_, index) => entered[index] ?? '')
  const last = Array.from({ length: 4 }, (_, index) => entered[index + 4] ? '*' : '')

  return <div className="phone-display" aria-label={`전화번호 뒷자리 ${entered.length}자리 입력됨`}>
    <span>{PHONE_PREFIX}</span><b aria-hidden="true">-</b>
    <span className="phone-slots" aria-hidden="true">{middle.map((value, index) => <i key={`middle-${index}`}>{value}</i>)}</span>
    <b aria-hidden="true">-</b>
    <span className="phone-slots masked" aria-hidden="true">{last.map((value, index) => <i key={`last-${index}`}>{value}</i>)}</span>
  </div>
}

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [step, setStep] = useState<LoginStep>('phone')
  const [phone, setPhone] = useState(PHONE_PREFIX)
  const [pin, setPin] = useState('')
  const [pending, setPending] = useState(false)

  const verifyPhone = async (nextPhone: string) => {
    setPending(true)
    try {
      const result = await checkPhone(nextPhone)
      if (!result.exists) {
        setPhone(PHONE_PREFIX)
        toast.error('등록된 전화번호가 없어요.')
        return
      }
      setStep('pin')
      toast.success('전화번호를 확인했어요.')
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '전화번호를 확인하지 못했어요.')
    } finally { setPending(false) }
  }

  const submitLogin = async (nextPin: string) => {
    setPending(true)
    try {
      await login(phone, nextPin)
      toast.success('로그인되었어요.')
      navigate('/', { replace: true })
    } catch {
      setPin('')
      toast.error('비밀번호가 맞지 않아요. 다시 입력해 주세요.')
    } finally { setPending(false) }
  }

  const enterDigit = (digit: string) => {
    if (pending) return
    if (step === 'phone') {
      if (phone.length >= PHONE_LENGTH) return
      const nextPhone = phone + digit
      setPhone(nextPhone)
      if (nextPhone.length === PHONE_LENGTH) void verifyPhone(nextPhone)
      return
    }
    if (pin.length >= PIN_LENGTH) return
    const nextPin = pin + digit
    setPin(nextPin)
    if (nextPin.length === PIN_LENGTH) void submitLogin(nextPin)
  }

  const eraseDigit = () => {
    if (pending) return
    if (step === 'phone') setPhone((value) => value.length > PHONE_PREFIX.length ? value.slice(0, -1) : value)
    else setPin((value) => value.slice(0, -1))
  }

  return <AppShell className="login-canvas" label="한마디 간편 로그인">
    <section className="login-panel" aria-busy={pending}>
      <div className="login-brand"><span className="brand-mark">한</span><strong>한마디</strong></div>
      <div className="login-character"><CharacterImage src={mainCharacter} alt="한마디 금융 안내 도우미" /></div>
      <div className="login-copy"><span className="lock-badge"><Icon name="lock" /></span><h1>{step === 'phone' ? <>전화번호를<br />입력해 주세요</> : <>간편 비밀번호를<br />입력해 주세요</>}</h1><p>{step === 'phone' ? <>등록된 휴대전화 번호<br />11자리를 확인할게요.</> : <><strong className="verified-phone">{formatPhone(phone)}</strong><br />6자리 숫자로 확인할게요.</>}</p></div>
      {step === 'phone'
        ? <PhoneDisplay phone={phone} />
        : <div className="pin-dots" aria-label={`비밀번호 ${pin.length}자리 입력됨`}>{Array.from({ length: PIN_LENGTH }, (_, index) => <span key={index} className={index < pin.length ? 'filled' : ''} />)}</div>}
      <p className="login-status" role="status">{pending ? (step === 'phone' ? '전화번호를 확인하고 있어요…' : '로그인하고 있어요…') : '\u00a0'}</p>
      <div className="pin-keypad" aria-label="숫자 키패드">
        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((number) => <button key={number} type="button" disabled={pending} onClick={() => enterDigit(String(number))}>{number}</button>)}
        <span aria-hidden="true" /><button type="button" disabled={pending} onClick={() => enterDigit('0')}>0</button><button type="button" disabled={pending} aria-label="한 자리 지우기" onClick={eraseDigit}><Icon name="backspace" /></button>
      </div>
      {step === 'pin' && <button type="button" className="change-phone-button" disabled={pending} onClick={() => { setStep('phone'); setPhone(PHONE_PREFIX); setPin('') }}>전화번호 다시 입력하기</button>}
    </section>
  </AppShell>
}
