import { create } from 'zustand'

export type ToastSeverity = 'success' | 'error' | 'warning' | 'info'
type ToastState = { open: boolean; message: string; severity: ToastSeverity; show: (message: string, severity?: ToastSeverity) => void; close: () => void }

export const useToastStore = create<ToastState>((set) => ({
  open: false, message: '', severity: 'info',
  show: (message, severity = 'info') => set({ open: true, message, severity }),
  close: () => set({ open: false }),
}))

export const toast = {
  success: (message: string) => useToastStore.getState().show(message, 'success'),
  error: (message: string) => useToastStore.getState().show(message, 'error'),
  warning: (message: string) => useToastStore.getState().show(message, 'warning'),
  info: (message: string) => useToastStore.getState().show(message, 'info'),
}
