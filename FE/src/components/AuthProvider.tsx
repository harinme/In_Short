import { useEffect, useState, type ReactNode } from 'react'
import { getMe, login as requestLogin, logout as requestLogout, type AuthUser } from '../api/auth'
import { AuthContext } from '../auth-context'
import { toast } from '../stores/toast'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [initialized, setInitialized] = useState(false)
  useEffect(() => { void getMe().then(setUser).catch(() => toast.error('로그인 상태를 확인하지 못했어요.')).finally(() => setInitialized(true)) }, [])
  const login = async (phone: string, pin: string) => { setUser(await requestLogin(phone, pin)) }
  const logout = async () => { await requestLogout(); setUser(null) }
  return <AuthContext.Provider value={{ user, initialized, login, logout }}>{children}</AuthContext.Provider>
}
