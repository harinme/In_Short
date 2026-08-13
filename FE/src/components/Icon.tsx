import type { ReactNode } from 'react'

export type IconName = 'home' | 'transfer' | 'history' | 'profile' | 'headset' | 'wallet' | 'shield' | 'bell' | 'close' | 'mic' | 'backspace' | 'logout' | 'lock' | 'eye' | 'refresh' | 'chevron'

export function Icon({ name }: { name: IconName }) {
  const paths: Record<IconName, ReactNode> = {
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
    backspace: <><path d="m9 7-5 5 5 5h11V7H9Z"/><path d="m13 10 4 4M17 10l-4 4"/></>,
    logout: <><path d="M10 5H5v14h5M14 8l4 4-4 4M9 12h9"/></>,
    lock: <><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/></>,
    eye: <><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"/><circle cx="12" cy="12" r="2.5"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M6.1 8.5A7 7 0 0 1 18.5 7L20 12M4 12l1.5 5A7 7 0 0 0 18 15.5"/></>,
    chevron: <path d="m9 5 7 7-7 7"/>,
  }
  return <svg viewBox="0 0 24 24" aria-hidden="true">{paths[name]}</svg>
}
