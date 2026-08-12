import { useState } from 'react'
import mainCharacter from './assets/main-character.png'
import questionCharacter from './assets/question-character.png'
import './App.css'

type IconName = 'home' | 'transfer' | 'history' | 'profile' | 'headset' | 'wallet' | 'shield' | 'bell' | 'close' | 'mic'
type VoiceStep = 'listening' | 'confirming' | 'responding'

function Icon({ name }: { name: IconName }) {
  const paths: Record<IconName, React.ReactNode> = {
    home: <><path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10.5V20h13v-9.5M9 20v-6h6v6"/></>,
    transfer: <><path d="M7 7h12l-3-3M17 17H5l3 3"/><path d="m19 7-3 3M5 17l3-3"/></>,
    history: <><path d="M4.9 6.9A8 8 0 1 1 4 13"/><path d="M4 4v5h5M12 8v5l3 2"/></>,
    profile: <><circle cx="12" cy="8" r="4"/><path d="M4.5 21a7.5 7.5 0 0 1 15 0"/></>,
    headset: <><path d="M4 14v-2a8 8 0 0 1 16 0v2"/><path d="M4 14a2 2 0 0 1 2-2h1v6H6a2 2 0 0 1-2-2v-2ZM20 14a2 2 0 0 0-2-2h-1v6h1a2 2 0 0 0 2-2v-2ZM17 20c-1 1-2.5 1.5-4 1.5"/></>,
    wallet: <><path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H19v14H5.5A2.5 2.5 0 0 1 3 16.5v-9Z"/><path d="M16 10h5v5h-5a2.5 2.5 0 0 1 0-5Z"/></>,
    shield: <><path d="M12 3 5 6v5c0 4.6 2.8 8 7 10 4.2-2 7-5.4 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/></>,
    bell: <><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 8h18c0-1-3-1-3-8ZM10 21h4"/></>,
    close: <><path d="m6 6 12 12M18 6 6 18"/></>,
    mic: <><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3M9 21h6"/></>,
  }
  return <svg viewBox="0 0 24 24" aria-hidden="true">{paths[name]}</svg>
}

const quickActions = [
  { icon: 'transfer' as const, label: '간편 송금', detail: '크고 쉽게 보내기' },
  { icon: 'wallet' as const, label: '내 자산', detail: '한눈에 확인하기' },
  { icon: 'shield' as const, label: '안심 금융', detail: '사기 예방 도움받기' },
]

function App() {
  const [notice, setNotice] = useState('')
  const [voiceMode, setVoiceMode] = useState(false)
  const [voiceStep, setVoiceStep] = useState<VoiceStep>('listening')

  const announce = (label: string) => setNotice(`${label} 메뉴를 선택했습니다.`)
  const startVoice = () => { setVoiceMode(true); setVoiceStep('listening'); setNotice('음성 상담을 시작합니다.') }
  const exitVoice = () => { setVoiceMode(false); setVoiceStep('listening'); setNotice('음성 상담을 종료했습니다.') }

  const simulateRecognition = () => {
    if (voiceStep === 'listening') {
      setVoiceStep('confirming')
      setNotice('말씀하신 내용을 확인해 주세요.')
    } else if (voiceStep === 'responding') {
      setVoiceStep('listening')
      setNotice('다음 말씀을 듣고 있습니다.')
    }
  }

  const confirmSpeech = () => {
    setVoiceStep('responding')
    setNotice('확인했습니다. 송금 안내를 시작합니다.')
  }

  return (
    <div className="app-shell">
      <main className={`mobile-canvas ${voiceMode ? 'voice-mode' : ''}`} aria-label="한마디 메인 화면">
        {voiceMode ? (
          <section className="voice-conversation" aria-label="음성 상담 화면">
            <header className="voice-header">
              <div><span className="live-dot" aria-hidden="true" /><strong>한마디와 대화 중</strong></div>
              <button className="close-button" type="button" aria-label="음성 상담 종료" onClick={exitVoice}><Icon name="close" /></button>
            </header>

            <div className={`conversation-bubble ${voiceStep}`} role="status" aria-live="polite">
              {voiceStep === 'listening' && <><span>편하게 말씀해 주세요</span><strong>제가 듣고 있어요.</strong></>}
              {voiceStep === 'confirming' && <><span>이 말씀이 맞나요?</span><strong>“김민수에게 5만원 보내줘”</strong></>}
              {voiceStep === 'responding' && <><span>네, 확인했어요</span><strong>김민수님께 5만원 송금을 도와드릴게요.</strong></>}
            </div>

            <div className="voice-character-wrap">
              <span className="pulse-ring ring-one" aria-hidden="true" />
              <span className="pulse-ring ring-two" aria-hidden="true" />
              <div className={`voice-character ${voiceStep === 'confirming' ? 'question-character' : ''}`}>
                <img
                  src={voiceStep === 'confirming' ? questionCharacter : mainCharacter}
                  alt={voiceStep === 'confirming' ? '질문하며 확인하는 금융 안내 도우미 한마디' : '말을 듣고 있는 금융 안내 도우미 한마디'}
                />
              </div>
            </div>

            <div className="voice-controls">
              {voiceStep === 'confirming' ? (
                <div className="confirm-actions">
                  <button type="button" className="secondary-action" onClick={() => setVoiceStep('listening')}>다시 말할게요</button>
                  <button type="button" className="primary-action" onClick={confirmSpeech}>네, 맞아요</button>
                </div>
              ) : (
                <button type="button" className={`mic-button ${voiceStep}`} onClick={simulateRecognition} aria-label={voiceStep === 'listening' ? '음성 인식 예시 실행' : '계속 대화하기'}>
                  <Icon name="mic" />
                </button>
              )}
              <p>{voiceStep === 'listening' ? '말씀하시면 자동으로 알아들어요' : voiceStep === 'responding' ? '안내가 끝나면 다시 말씀해 주세요' : '내용을 확인해 주세요'}</p>
            </div>
          </section>
        ) : (
          <>
            <header className="top-bar">
              <div><p className="eyebrow">안녕하세요, 김한마디님</p><h1>오늘도 편안한 금융생활</h1></div>
              <button className="icon-button" type="button" aria-label="알림 보기" onClick={() => announce('알림')}><Icon name="bell" /><span className="notification-dot" aria-hidden="true" /></button>
            </header>

            <section className="assistant-card" aria-labelledby="assistant-title">
              <div className="speech-bubble"><p id="assistant-title">무엇을 도와드릴까요?</p><strong>말씀만 하세요. 제가 함께할게요.</strong></div>
              <div className="character-frame"><img src={mainCharacter} alt="금융 안내 도우미 한마디" /></div>
              <button className="voice-button" type="button" onClick={startVoice}><Icon name="headset" /><span>말로 상담하기</span></button>
            </section>

            <section className="quick-section" aria-labelledby="quick-title">
              <div className="section-heading"><h2 id="quick-title">자주 찾는 메뉴</h2><span>원하는 기능을 눌러보세요</span></div>
              <div className="quick-grid">
                {quickActions.map((action) => <button key={action.label} type="button" className="quick-card" onClick={() => announce(action.label)}><span className="quick-icon"><Icon name={action.icon} /></span><strong>{action.label}</strong><small>{action.detail}</small></button>)}
              </div>
            </section>

            <nav className="bottom-nav" aria-label="주요 메뉴">
              <button className="active" type="button" aria-current="page" onClick={() => announce('홈')}><Icon name="home" /><span>홈</span></button>
              <button type="button" onClick={() => announce('송금')}><Icon name="transfer" /><span>송금</span></button>
              <button type="button" onClick={() => announce('내역')}><Icon name="history" /><span>내역</span></button>
              <button type="button" onClick={() => announce('내 정보')}><Icon name="profile" /><span>내 정보</span></button>
            </nav>
          </>
        )}
        <p className="sr-only" role="status" aria-live="polite">{notice}</p>
      </main>
    </div>
  )
}

export default App
