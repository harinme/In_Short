import { useEffect, useRef, useState } from 'react'
import type { MicVAD } from '@ricky0123/vad-web'
import { useNavigate } from 'react-router-dom'
import type { VoiceInterpretation } from '../api/voice'
import { createVoiceConversation, deleteVoiceConversation, transcribeAudio } from '../api/voice'
import { classifyVoiceIntent } from '../api/intent'
import { AppShell } from '../components/AppShell'
import { CharacterImage } from '../components/CharacterImage'
import { Icon } from '../components/Icon'
import mainCharacter from '../assets/main-character.png'
import questionCharacter from '../assets/question-character.png'

type VoiceStep = 'ready' | 'recording' | 'processing' | 'confirming' | 'routing' | 'responding' | 'error'

async function resolveInterpretation(
  conversationId: string,
  result: Awaited<ReturnType<typeof transcribeAudio>>,
): Promise<VoiceInterpretation> {
  if (result.interpretation) return result.interpretation
  const { intent } = await classifyVoiceIntent(result.transcript)
  const nextAction = intent === 'TRANSFER' ? 'OPEN_TRANSFER' : intent === 'BALANCE' ? 'OPEN_ACCOUNTS' : intent === 'HISTORY' ? 'OPEN_HISTORY' : 'RETRY'
  return {
    conversationId,
    requestId: result.requestId,
    transcript: result.transcript,
    intent,
    status: intent === 'UNKNOWN' ? 'UNSUPPORTED' : 'READY',
    nextAction,
    slots: { transfer: null, balance: null, history: null },
    missingFields: [],
    message: intent === 'UNKNOWN' ? '요청하신 업무를 확인하지 못했어요.' : '요청하신 화면으로 이동할게요.',
  }
}

function float32ToWav(audio: Float32Array) {
  const buffer = new ArrayBuffer(44 + audio.length * 2)
  const view = new DataView(buffer)
  const writeText = (offset: number, value: string) => {
    for (let index = 0; index < value.length; index += 1) view.setUint8(offset + index, value.charCodeAt(index))
  }

  writeText(0, 'RIFF')
  view.setUint32(4, 36 + audio.length * 2, true)
  writeText(8, 'WAVE')
  writeText(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, 16_000, true)
  view.setUint32(28, 32_000, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeText(36, 'data')
  view.setUint32(40, audio.length * 2, true)

  for (let index = 0; index < audio.length; index += 1) {
    const sample = Math.max(-1, Math.min(1, audio[index]))
    view.setInt16(44 + index * 2, sample < 0 ? sample * 32768 : sample * 32767, true)
  }
  return new Blob([buffer], { type: 'audio/wav' })
}

function mergeAudioFrames(frames: Float32Array[]) {
  const length = frames.reduce((total, frame) => total + frame.length, 0)
  const audio = new Float32Array(length)
  let offset = 0
  frames.forEach((frame) => {
    audio.set(frame, offset)
    offset += frame.length
  })
  return audio
}

function speakText(text: string) {
  return new Promise<void>((resolve) => {
    if (!('speechSynthesis' in window)) {
      resolve()
      return
    }

    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'ko-KR'
    utterance.rate = 0.85
    utterance.pitch = 1
    utterance.volume = 1
    utterance.onend = () => resolve()
    utterance.onerror = () => resolve()
    window.speechSynthesis.speak(utterance)
  })
}

export function VoicePage() {
  const navigate = useNavigate()
  const [voiceStep, setVoiceStep] = useState<VoiceStep>('ready')
  const [transcript, setTranscript] = useState('')
  const [interpretation, setInterpretation] = useState<VoiceInterpretation | null>(null)
  const [speechDetected, setSpeechDetected] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [vadLoading, setVadLoading] = useState(false)
  const conversationIdRef = useRef<string | null>(null)
  const closingRef = useRef(false)
  const processingRef = useRef(false)
  const vadRef = useRef<MicVAD | null>(null)
  const speechDetectedRef = useRef(false)
  const silenceStartedAtRef = useRef<number | null>(null)
  const speechTimeoutRef = useRef<number | null>(null)
  const audioFramesRef = useRef<Float32Array[]>([])

  const clearSpeechTimeout = () => {
    if (speechTimeoutRef.current !== null) {
      window.clearTimeout(speechTimeoutRef.current)
      speechTimeoutRef.current = null
    }
  }

  const ensureConversation = async () => {
    if (conversationIdRef.current) return conversationIdRef.current
    const conversation = await createVoiceConversation()
    conversationIdRef.current = conversation.conversationId
    return conversation.conversationId
  }

  const processRecording = async (blob: Blob) => {
    if (closingRef.current) return
    setVoiceStep('processing')
    try {
      const conversationId = await ensureConversation()
      const result = await transcribeAudio(conversationId, blob)
      if (closingRef.current) return
      const resolvedInterpretation = await resolveInterpretation(conversationId, result)
      if (closingRef.current) return
      setTranscript(result.transcript)
      setInterpretation(resolvedInterpretation)
      setVoiceStep('confirming')
    } catch (error) {
      if (closingRef.current) return
      setErrorMessage(error instanceof Error ? error.message : '음성을 인식하지 못했습니다.')
      setVoiceStep('error')
    }
  }

  const finishSpeech = () => {
    if (!speechDetectedRef.current || processingRef.current || closingRef.current) return
    const audio = mergeAudioFrames(audioFramesRef.current)
    if (audio.length === 0) return

    processingRef.current = true
    speechDetectedRef.current = false
    audioFramesRef.current = []
    clearSpeechTimeout()
    silenceStartedAtRef.current = null
    setSpeechDetected(false)
    setVoiceStep('processing')
    void vadRef.current?.pause()
    void processRecording(float32ToWav(audio))
  }

  const initializeVad = async () => {
    if (vadRef.current) return vadRef.current
    setVadLoading(true)
    try {
      const { MicVAD } = await import('@ricky0123/vad-web')
      const microphoneVad = await MicVAD.new({
        startOnLoad: false,
        model: 'v5',
        baseAssetPath: 'https://cdn.jsdelivr.net/npm/@ricky0123/vad-web@0.0.29/dist/',
        onnxWASMBasePath: 'https://cdn.jsdelivr.net/npm/onnxruntime-web@1.22.0/dist/',
        positiveSpeechThreshold: 0.58,
        negativeSpeechThreshold: 0.36,
        redemptionMs: 1000,
        minSpeechMs: 160,
        preSpeechPadMs: 256,
        submitUserSpeechOnPause: true,
        onSpeechStart: () => {
          speechDetectedRef.current = true
          audioFramesRef.current = []
          silenceStartedAtRef.current = null
          clearSpeechTimeout()
          speechTimeoutRef.current = window.setTimeout(finishSpeech, 6_000)
          setSpeechDetected(true)
          setVoiceStep('recording')
        },
        onFrameProcessed: (probabilities, frame) => {
          if (!speechDetectedRef.current || processingRef.current || closingRef.current) return
          audioFramesRef.current.push(frame.slice())
          if (probabilities.isSpeech >= 0.58) {
            silenceStartedAtRef.current = null
            return
          }
          silenceStartedAtRef.current ??= performance.now()
          if (performance.now() - silenceStartedAtRef.current >= 1000) {
            finishSpeech()
          }
        },
        onSpeechEnd: async (audio) => {
          if (processingRef.current || closingRef.current) return
          processingRef.current = true
          audioFramesRef.current = []
          clearSpeechTimeout()
          speechDetectedRef.current = false
          silenceStartedAtRef.current = null
          setSpeechDetected(false)
          await vadRef.current?.pause()
          await processRecording(float32ToWav(audio))
        },
        onVADMisfire: () => {
          if (!processingRef.current && !closingRef.current) {
            clearSpeechTimeout()
            audioFramesRef.current = []
            setSpeechDetected(false)
            speechDetectedRef.current = false
            silenceStartedAtRef.current = null
            setVoiceStep('recording')
          }
        },
      })
      vadRef.current = microphoneVad
      return microphoneVad
    } finally {
      setVadLoading(false)
    }
  }

  useEffect(() => {
    closingRef.current = false
    return () => {
      closingRef.current = true
      clearSpeechTimeout()
      audioFramesRef.current = []
      void vadRef.current?.destroy()
    }
  }, [])

  useEffect(() => {
    void speakText('안녕하세요! 오늘은 어떤 걸 도와드릴까요?')
    return () => window.speechSynthesis?.cancel()
  }, [])

  useEffect(() => {
    if (voiceStep !== 'confirming' || !transcript) return
    void speakText(`제가 들은 내용은 ${transcript}입니다. 맞나요?`)
    return () => window.speechSynthesis?.cancel()
  }, [transcript, voiceStep])

  const startRecording = async () => {
    window.speechSynthesis?.cancel()
    setVadLoading(true)
    setErrorMessage('')
    setTranscript('')
    setInterpretation(null)
    setSpeechDetected(false)
    speechDetectedRef.current = false
    audioFramesRef.current = []
    clearSpeechTimeout()
    try {
      await ensureConversation()
      await speakText('이제 말씀해 주세요. 말씀을 마치면 자동으로 인식할게요.')
      const microphoneVad = await initializeVad()
      setVadLoading(false)
      if (microphoneVad.errored) throw new Error(microphoneVad.errored)
      processingRef.current = false
      await microphoneVad.start()
      setVoiceStep('recording')
    } catch (error) {
      setVadLoading(false)
      setErrorMessage(error instanceof Error ? error.message : '마이크를 시작하지 못했습니다.')
      setVoiceStep('error')
    }
  }

  const closeConversation = async () => {
    window.speechSynthesis?.cancel()
    closingRef.current = true
    clearSpeechTimeout()
    audioFramesRef.current = []
    await vadRef.current?.destroy()
    vadRef.current = null
    const conversationId = conversationIdRef.current
    conversationIdRef.current = null
    if (conversationId) await deleteVoiceConversation(conversationId).catch(() => undefined)
    navigate('/')
  }

  const retry = () => {
    window.speechSynthesis?.cancel()
    clearSpeechTimeout()
    void vadRef.current?.pause()
    setVoiceStep('ready')
    setErrorMessage('')
    setTranscript('')
    setInterpretation(null)
    setSpeechDetected(false)
    speechDetectedRef.current = false
    audioFramesRef.current = []
  }

  const continueWithIntent = async () => {
    setVoiceStep('routing')
    try {
      if (!interpretation) throw new Error('업무 해석 결과가 없습니다. 다시 말씀해 주세요.')

      if (interpretation.nextAction === 'OPEN_TRANSFER') {
        await speakText('송금 화면으로 이동할게요.')
        return navigate('/transfer/new', { state: interpretation.slots.transfer })
      }
      if (interpretation.nextAction === 'OPEN_ACCOUNTS') {
        await speakText('내 계좌를 확인해 드릴게요.')
        return navigate('/accounts', { state: interpretation.slots.balance })
      }
      if (interpretation.nextAction === 'OPEN_HISTORY') {
        await speakText('거래 내역을 확인해 드릴게요.')
        return navigate('/accounts', { state: interpretation.slots.history })
      }

      setVoiceStep('responding')
      await speakText(interpretation.message)
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '업무를 확인하지 못했습니다.')
      setVoiceStep('error')
    }
  }

  const isQuestioning = voiceStep === 'confirming'

  return <AppShell className="voice-mode" label="음성 상담 화면"><section className="voice-conversation">
    <header className="voice-header"><div><span className={`live-dot ${voiceStep === 'recording' ? 'recording' : ''}`} aria-hidden="true" /><strong>실시간으로 대화 중</strong></div><button className="close-button" type="button" aria-label="음성 상담 종료" onClick={() => void closeConversation()}><Icon name="close" /></button></header>
    <div className={`conversation-bubble ${voiceStep}`} role="status" aria-live="polite">
      {voiceStep === 'ready' && <><span>편하게 말씀해 주세요</span><strong>마이크를 누르면 듣기 시작해요.</strong></>}
      {voiceStep === 'recording' && <><span>마이크가 켜져 있어요</span><strong>{speechDetected ? '목소리가 감지됐어요.' : '말씀해 주세요…'}</strong><small>말을 마치면 자동으로 전송할게요.</small></>}
      {voiceStep === 'processing' && <><span>음성을 백엔드로 보냈어요</span><strong>최종 문장을 확인하고 있어요.</strong></>}
      {voiceStep === 'confirming' && <><span>이 말씀이 맞나요?</span><strong>“{transcript}”</strong></>}
      {voiceStep === 'routing' && <><span>원하시는 업무를 확인하고 있어요</span><strong>잠시만 기다려 주세요.</strong></>}
      {voiceStep === 'responding' && <><span>업무를 정확히 확인하지 못했어요</span><strong>송금 또는 계좌 확인처럼 다시 말씀해 주세요.</strong></>}
      {voiceStep === 'error' && <><span>다시 확인이 필요해요</span><strong>{errorMessage}</strong></>}
    </div>
    <div className="voice-character-wrap"><span className="pulse-ring ring-one" aria-hidden="true" /><span className="pulse-ring ring-two" aria-hidden="true" /><div className={`voice-character ${isQuestioning ? 'question-character' : ''}`}><CharacterImage src={isQuestioning ? questionCharacter : mainCharacter} alt={isQuestioning ? '질문하며 확인하는 금융 안내 도우미 캐릭터' : '말을 듣고 있는 금융 안내 도우미 캐릭터'} /></div></div>
    <div className="voice-controls">
      {voiceStep === 'confirming' ? <div className="confirm-actions"><button type="button" className="secondary-action" onClick={retry}>다시 말할게요</button><button type="button" className="primary-action" onClick={() => void continueWithIntent()}>네, 맞아요</button></div> : voiceStep === 'error' ? <button type="button" className="retry-voice-button" onClick={() => void startRecording()}>다시 말하기</button> : <button type="button" className={`mic-button ${voiceStep}`} disabled={voiceStep === 'recording' || voiceStep === 'processing' || voiceStep === 'routing' || vadLoading} onClick={voiceStep === 'ready' || voiceStep === 'responding' ? () => void startRecording() : undefined} aria-label={voiceStep === 'recording' ? '음성을 듣고 있어요' : '녹음 시작하기'}><Icon name="mic" /></button>}
      <p>{voiceStep === 'ready' ? vadLoading ? '음성 감지 기능을 준비하고 있어요' : '마이크 버튼을 눌러 말씀해 주세요' : voiceStep === 'recording' ? '말을 마치면 자동으로 인식해요' : voiceStep === 'processing' ? '음성을 글자로 바꾸고 있어요' : voiceStep === 'confirming' ? '인식된 내용이 맞는지 확인해 주세요' : voiceStep === 'routing' ? '맞는 화면으로 이동할게요' : voiceStep === 'error' ? '버튼을 눌러 다시 시도할 수 있어요' : '마이크를 눌러 다시 말씀해 주세요'}</p>
    </div>
  </section></AppShell>
}
