import type { ReactNode } from 'react'

type AppShellProps = {
  children: ReactNode
  className?: string
  label: string
}

export function AppShell({ children, className = '', label }: AppShellProps) {
  const canvasClassName = ['mobile-canvas', className].filter(Boolean).join(' ')
  return <div className="app-shell"><main className={canvasClassName} aria-label={label}>{children}</main></div>
}
