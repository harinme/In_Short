import { Icon, type IconName } from './Icon'
import { useNavigate } from 'react-router-dom'

const navigationItems: Array<{ icon: IconName; label: string }> = [
  { icon: 'home', label: '홈' },
  { icon: 'transfer', label: '송금' },
  { icon: 'history', label: '내역' },
  { icon: 'profile', label: '내 정보' },
]

export function BottomNavigation() {
  const navigate = useNavigate()
  return <nav className="bottom-nav" aria-label="주요 메뉴">{navigationItems.map((item, index) => <button key={item.label} className={index === 0 ? 'active' : ''} type="button" aria-current={index === 0 ? 'page' : undefined} onClick={() => item.label === '홈' ? navigate('/') : item.label === '송금' ? navigate('/transfer/new') : undefined}><Icon name={item.icon} /><span>{item.label}</span></button>)}</nav>
}
