import type { ButtonHTMLAttributes } from 'react'
import { Icon, type IconName } from './Icon'

type IconButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  icon: IconName
}

export function IconButton({ children, className = '', icon, type = 'button', ...props }: IconButtonProps) {
  return <button className={`icon-button ${className}`.trim()} type={type} {...props}><Icon name={icon} />{children}</button>
}
