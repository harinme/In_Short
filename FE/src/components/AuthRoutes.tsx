import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth-context'

export function ProtectedRoute() {
  const { user, initialized } = useAuth()
  if (!initialized) return <div className="auth-loading" role="status">로그인 상태를 확인하고 있어요.</div>
  return user ? <Outlet /> : <Navigate to="/login" replace />
}

export function PublicOnlyRoute() {
  const { user, initialized } = useAuth()
  if (!initialized) return <div className="auth-loading" role="status">로그인 상태를 확인하고 있어요.</div>
  return user ? <Navigate to="/" replace /> : <Outlet />
}
