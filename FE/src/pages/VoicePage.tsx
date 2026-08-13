import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createVoiceConversation, deleteVoiceConversation, transcribeAudio } from '../api/voice'
import { AppShell } from '../components/AppShell'
import { CharacterImage } from '../components/CharacterImage'
import { Icon } from '../components/Icon'
import mainCharacter from '../assets/main-character.png'
import questionCharacter from '../assets/question-character.png'

type VoiceStep = 'ready' | 'recording' | 'processing' | 'confirming' | 'responding' | 'error'

export function VoicePage() {
  const navigate = useNavigate()
  const [voiceStep, setVoiceStep] = useState<VoiceStep>('ready')
  const [transcript, setTranscript] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const conversationIdRef = useRef<string | null>(null)
  const recorderRef = useRef<MediaRecorder | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const chunksRef = useRef<BlobPart[]>([])
  const closingRef = useRef(false)

  const ensureConversation = async () => {
    if (conversationIdRef.current) return conversationIdRef.current
    const conversation = await createVoiceConversation()
    conversationIdRef.current = conversation.conversationId
    return conversation.conversationId
  }

  const releaseMicrophone = () => {
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    recorderRef.current = null
  }

  useEffect(() => () => {
    closingRef.current = true
    releaseMicrophone()
  }, [])

  const processRecording = async (blob: Blob) => {
    if (closingRef.current) return
    setVoiceStep('processing')
    try {
      const conversationId = await ensureConversation()
      const result = await transcribeAudio(conversationId, blob)
      if (closingRef.current) return
      setTranscript(result.transcript)
      setVoiceStep('confirming')
    } catch (error) {
      if (closingRef.current) return
      setErrorMessage(error instanceof Error ? error.message : '음성을 인식하지 못했습니다.')
      setVoiceStep('error')
    }
  }

  const startRecording = async () => {
    setErrorMessage('')
    setTranscript('')
    try {
      if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') throw new Error('이 브라우저에서는 음성 녹음을 사용할 수 없습니다.')
      await ensureConversation()
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      chunksRef.current = []
      const preferredType = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'].find((type) => MediaRecorder.isTypeSupported(type))
      const recorder = preferredType ? new MediaRecorder(stream, { mimeType: preferredType }) : new MediaRecorder(stream)
      recorderRef.current = recorder
      recorder.ondataavailable = (event) => { if (event.data.size > 0) chunksRef.current.push(event.data) }
      recorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' })
        releaseMicrophone()
        if (closingRef.current) return
        if (blob.size === 0) {
          setErrorMessage('녹음된 음성이 없습니다. 다시 말씀해 주세요.')
          setVoiceStep('error')
          return
        }
        void processRecording(blob)
      }
      recorder.start()
      setVoiceStep('recording')
    } catch (error) {
      releaseMicrophone()
      const denied = error instanceof DOMException && error.name === 'NotAllowedError'
      setErrorMessage(denied ? '마이크 사용을 허용해 주세요.' : error instanceof Error ? error.message : '마이크를 시작하지 못했습니다.')
      setVoiceStep('error')
    }
  }

  const stopRecording = () => {
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop()
  }

  const closeConversation = async () => {
    closingRef.current = true
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop()
    releaseMicrophone()
    const conversationId = conversationIdRef.current
    conversationIdRef.current = null
    if (conversationId) await deleteVoiceConversation(conversationId).catch(() => undefined)
    navigate('/')
  }

  const retry = () => {
    setVoiceStep('ready')
    setErrorMessage('')
    setTranscript('')
  }

  const isQuestioning = voiceStep === 'confirming'

  return <AppShell className="voice-mode" label="음성 상담 화면"><section className="voice-conversation">
    <header className="voice-header"><div><span className={`live-dot ${voiceStep === 'recording' ? 'recording' : ''}`} aria-hidden="true" /><strong>실시간으로 대화 중</strong></div><button className="close-button" type="button" aria-label="음성 상담 종료" onClick={() => void closeConversation()}><Icon name="close" /></button></header>
    <div className={`conversation-bubble ${voiceStep}`} role="status" aria-live="polite">
      {voiceStep === 'ready' && <><span>편하게 말씀해 주세요</span><strong>마이크를 누르면 듣기 시작해요.</strong></>}
      {voiceStep === 'recording' && <><span>듣고 있어요</span><strong>말씀을 마치면 마이크를 다시 눌러 주세요.</strong></>}
      {voiceStep === 'processing' && <><span>말씀을 확인하고 있어요</span><strong>잠시만 기다려 주세요.</strong></>}
      {voiceStep === 'confirming' && <><span>이 말씀이 맞나요?</span><strong>“{transcript}”</strong></>}
      {voiceStep === 'responding' && <><span>네, 확인했어요</span><strong>말씀하신 내용을 기준으로 안내해 드릴게요.</strong></>}
      {voiceStep === 'error' && <><span>다시 확인이 필요해요</span><strong>{errorMessage}</strong></>}
    </div>
    <div className="voice-character-wrap"><span className="pulse-ring ring-one" aria-hidden="true" /><span className="pulse-ring ring-two" aria-hidden="true" /><div className={`voice-character ${isQuestioning ? 'question-character' : ''}`}><CharacterImage src={isQuestioning ? questionCharacter : mainCharacter} alt={isQuestioning ? '질문하며 확인하는 금융 안내 도우미 캐릭터' : '말을 듣고 있는 금융 안내 도우미 캐릭터'} /></div></div>
    <div className="voice-controls">
      {voiceStep === 'confirming' ? <div className="confirm-actions"><button type="button" className="secondary-action" onClick={retry}>다시 말할게요</button><button type="button" className="primary-action" onClick={() => setVoiceStep('responding')}>네, 맞아요</button></div> : voiceStep === 'error' ? <button type="button" className="retry-voice-button" onClick={() => void startRecording()}>다시 말하기</button> : <button type="button" className={`mic-button ${voiceStep}`} disabled={voiceStep === 'processing'} onClick={voiceStep === 'recording' ? stopRecording : voiceStep === 'ready' || voiceStep === 'responding' ? () => void startRecording() : undefined} aria-label={voiceStep === 'recording' ? '녹음 끝내기' : '녹음 시작하기'}><Icon name="mic" /></button>}
      <p>{voiceStep === 'ready' ? '마이크 버튼을 눌러 말씀해 주세요' : voiceStep === 'recording' ? '녹음 중 · 다시 누르면 멈춰요' : voiceStep === 'processing' ? '음성을 글자로 바꾸고 있어요' : voiceStep === 'confirming' ? '인식된 내용이 맞는지 확인해 주세요' : voiceStep === 'error' ? '버튼을 눌러 다시 시도할 수 있어요' : '계속 말씀하려면 마이크를 눌러 주세요'}</p>
    </div>
  </section></AppShell>
}
