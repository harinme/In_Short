import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { CharacterImage } from '../components/CharacterImage'
import { Icon } from '../components/Icon'
import mainCharacter from '../assets/main-character.png'
import questionCharacter from '../assets/question-character.png'

type VoiceStep = 'listening' | 'confirming' | 'responding'

export function VoicePage() {
  const navigate = useNavigate()
  const [voiceStep, setVoiceStep] = useState<VoiceStep>('listening')
  const advance = () => setVoiceStep((step) => step === 'listening' ? 'confirming' : 'listening')

  return <AppShell className="voice-mode" label="음성 상담 화면"><section className="voice-conversation">
    <header className="voice-header"><div><span className="live-dot" aria-hidden="true" /><strong>한마디와 대화 중</strong></div><button className="close-button" type="button" aria-label="음성 상담 종료" onClick={() => navigate('/')}><Icon name="close" /></button></header>
    <div className={`conversation-bubble ${voiceStep}`} role="status" aria-live="polite">{voiceStep === 'listening' && <><span>편하게 말씀해 주세요</span><strong>제가 듣고 있어요.</strong></>}{voiceStep === 'confirming' && <><span>이 말씀이 맞나요?</span><strong>“김민수에게 5만원 보내줘”</strong></>}{voiceStep === 'responding' && <><span>네, 확인했어요</span><strong>김민수님께 5만원 송금을 도와드릴게요.</strong></>}</div>
    <div className="voice-character-wrap"><span className="pulse-ring ring-one" aria-hidden="true" /><span className="pulse-ring ring-two" aria-hidden="true" /><div className={`voice-character ${voiceStep === 'confirming' ? 'question-character' : ''}`}><CharacterImage src={voiceStep === 'confirming' ? questionCharacter : mainCharacter} alt={voiceStep === 'confirming' ? '질문하며 확인하는 금융 안내 도우미 한마디' : '말을 듣고 있는 금융 안내 도우미 한마디'} /></div></div>
    <div className="voice-controls">{voiceStep === 'confirming' ? <div className="confirm-actions"><button type="button" className="secondary-action" onClick={() => setVoiceStep('listening')}>다시 말할게요</button><button type="button" className="primary-action" onClick={() => setVoiceStep('responding')}>네, 맞아요</button></div> : <button type="button" className={`mic-button ${voiceStep}`} onClick={advance} aria-label={voiceStep === 'listening' ? '음성 인식 예시 실행' : '계속 대화하기'}><Icon name="mic" /></button>}<p>{voiceStep === 'listening' ? '말씀하시면 자동으로 알아들어요' : voiceStep === 'responding' ? '안내가 끝나면 다시 말씀해 주세요' : '내용을 확인해 주세요'}</p></div>
  </section></AppShell>
}
