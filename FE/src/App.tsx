import { RouterProvider } from 'react-router-dom'
import { router } from './router'
import { AuthProvider } from './components/AuthProvider'
import { ToastContainer } from './components/ToastContainer'
import './App.css'

export default function App() {
  return <AuthProvider><RouterProvider router={router} /><ToastContainer /></AuthProvider>
}
