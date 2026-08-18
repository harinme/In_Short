import { createContext, useContext } from 'react'
import type { AuthUser } from './api/auth'

export type AuthContextValue = { user: AuthUser | null; initialized: boolean; login: (phone: string, pin: string) => Promise<void>; logout: () => Promise<void> }
export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
