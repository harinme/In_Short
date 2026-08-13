import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { saveAuthentication } from '../auth'
import { AppShell } from '../components/AppShell'
import { CharacterImage } from '../components/CharacterImage'
import { Icon } from '../components/Icon'
import mainCharacter from '../assets/main-character.png'

const DEMO_PIN = '1234'

export function LoginPage() {
  const navigate = useNavigate()
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')

  const enterDigit = (digit: string) => {
    if (pin.length >= 4) return
    const nextPin = pin + digit
    setPin(nextPin)
    setError('')
    if (nextPin.length !== 4) return
    window.setTimeout(() => {
      if (nextPin === DEMO_PIN) {
        saveAuthentication()
        navigate('/', { replace: true })
        return
      }
      setPin('')
      setError('비밀번호가 맞지 않아요. 다시 입력해 주세요.')
    }, 220)
  }

  return <AppShell className="login-canvas" label="한마디 간편 로그인">
    <section className="login-panel">
      <div className="login-brand"><span className="brand-mark">한</span><strong>한마디</strong></div>
      <div className="login-character"><CharacterImage src={mainCharacter} alt="한마디 금융 안내 도우미" /></div>
      <div className="login-copy"><span className="lock-badge"><Icon name="lock" /></span><h1>간편 비밀번호를<br />입력해 주세요</h1><p>안전한 금융생활을 위해<br />4자리 숫자로 확인할게요.</p></div>
      <div className={`pin-dots ${error ? 'has-error' : ''}`} aria-label={`비밀번호 ${pin.length}자리 입력됨`}>{[0, 1, 2, 3].map((index) => <span key={index} className={index < pin.length ? 'filled' : ''} />)}</div>
      <p className="pin-error" role="alert">{error || '\u00a0'}</p>
      <div className="pin-keypad" aria-label="숫자 키패드">
        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((number) => <button key={number} type="button" onClick={() => enterDigit(String(number))}>{number}</button>)}
        <span aria-hidden="true" /><button type="button" onClick={() => enterDigit('0')}>0</button><button type="button" aria-label="한 자리 지우기" onClick={() => { setPin((value) => value.slice(0, -1)); setError('') }}><Icon name="backspace" /></button>
      </div>
      <p className="demo-hint">테스트 비밀번호: 1234</p>
    </section>
  </AppShell>
}
