import { Navigate, Outlet } from 'react-router-dom'
import { isAuthenticated } from '../auth'

export function ProtectedRoute() {
  return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />
}

export function PublicOnlyRoute() {
  return isAuthenticated() ? <Navigate to="/" replace /> : <Outlet />
}
