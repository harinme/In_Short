import { Navigate, createBrowserRouter } from 'react-router-dom'
import { ProtectedRoute, PublicOnlyRoute } from '../components/AuthRoutes'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { VoicePage } from '../pages/VoicePage'
import { NewTransferPage } from '../pages/NewTransferPage'
import { AccountsPage } from '../pages/AccountsPage'
import { AccountDetailPage } from '../pages/AccountDetailPage'
import { TransactionDetailPage } from '../pages/TransactionDetailPage'

export const router = createBrowserRouter([
  { element: <PublicOnlyRoute />, children: [{ path: '/login', element: <LoginPage /> }] },
  { element: <ProtectedRoute />, children: [{ path: '/', element: <HomePage /> }, { path: '/voice', element: <VoicePage /> }, { path: '/transfer/new', element: <NewTransferPage /> }, { path: '/accounts', element: <AccountsPage /> }, { path: '/accounts/:accountNumber', element: <AccountDetailPage /> }, { path: '/accounts/:accountNumber/transactions/:transactionId', element: <TransactionDetailPage /> }] },
  { path: '*', element: <Navigate to="/" replace /> },
])
