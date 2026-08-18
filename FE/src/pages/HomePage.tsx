import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth-context'
import { AppShell } from '../components/AppShell'
import { BottomNavigation } from '../components/BottomNavigation'
import { CharacterImage } from '../components/CharacterImage'
import { Icon } from '../components/Icon'
import { IconButton } from '../components/IconButton'
import { QuickActionCard } from '../components/QuickActionCard'
import mainCharacter from '../assets/main-character.png'
import { toast } from '../stores/toast'

const quickActions = [
  { icon: 'transfer' as const, label: '간편 송금', detail: '크고 쉽게 보내기' },
  { icon: 'wallet' as const, label: '내 자산', detail: '한눈에 확인하기' },
  { icon: 'shield' as const, label: '안심 금융', detail: '사기 예방 도움받기' },
]

export function HomePage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const handleLogout = async () => {
    try {
      await logout()
      toast.success('로그아웃되었어요.')
      navigate('/login', { replace: true })
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '로그아웃하지 못했어요.')
    }
  }

  return <AppShell label="한마디 메인 화면">
    <header className="top-bar">
      <div><p className="eyebrow">안녕하세요, {user?.name}님</p><h1>오늘도 편안한 금융생활</h1></div>
      <div className="header-actions"><IconButton className="labeled-icon-button" icon="bell" aria-label="알림 보기"><span className="action-label">알림</span><span className="notification-dot" aria-hidden="true" /></IconButton><IconButton className="logout-button labeled-icon-button" icon="logout" aria-label="로그아웃" onClick={() => void handleLogout()}><span className="action-label">로그아웃</span></IconButton></div>
    </header>
    <section className="assistant-card" aria-labelledby="assistant-title">
      <div className="speech-bubble"><p id="assistant-title">무엇을 도와드릴까요?</p><strong>말씀만 하세요. 제가 함께할게요.</strong></div>
      <div className="character-frame"><CharacterImage src={mainCharacter} alt="금융 안내 도우미 한마디" /></div>
      <button className="voice-button" type="button" onClick={() => navigate('/voice')}><Icon name="headset" /><span>말로 상담하기</span></button>
    </section>
    <section className="quick-section" aria-labelledby="quick-title"><div className="section-heading"><h2 id="quick-title">자주 찾는 메뉴</h2><span>원하는 기능을 눌러보세요</span></div><div className="quick-grid">{quickActions.map((action) => <QuickActionCard key={action.label} {...action} onClick={action.label === '간편 송금' ? () => navigate('/transfer/new') : action.label === '내 자산' ? () => navigate('/accounts') : undefined} />)}</div></section>
    <BottomNavigation />
  </AppShell>
}
