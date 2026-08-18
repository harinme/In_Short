import Alert from '@mui/material/Alert'
import Snackbar from '@mui/material/Snackbar'
import { useToastStore } from '../stores/toast'

export function ToastContainer() {
  const { open, message, severity, close } = useToastStore()
  return <Snackbar open={open} autoHideDuration={4000} onClose={(_, reason) => { if (reason !== 'clickaway') close() }} anchorOrigin={{ vertical: 'top', horizontal: 'center' }} sx={{ top: { xs: 16, sm: 24 }, width: 'min(calc(100% - 32px), 358px)' }}>
    <Alert onClose={close} severity={severity} variant="filled" role="alert" sx={{ width: '100%', borderRadius: 3, fontWeight: 700 }}>{message}</Alert>
  </Snackbar>
}
