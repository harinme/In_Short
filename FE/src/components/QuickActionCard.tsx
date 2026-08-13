import { Icon, type IconName } from './Icon'

type QuickActionCardProps = {
  detail: string
  icon: IconName
  label: string
  onClick?: () => void
}

export function QuickActionCard({ detail, icon, label, onClick }: QuickActionCardProps) {
  return <button type="button" className="quick-card" onClick={onClick}><span className="quick-icon"><Icon name={icon} /></span><strong>{label}</strong><small>{detail}</small></button>
}
