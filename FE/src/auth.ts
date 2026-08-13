const AUTH_KEY = 'hanmadi-authenticated'

export const isAuthenticated = () => window.sessionStorage.getItem(AUTH_KEY) === 'true'
export const saveAuthentication = () => window.sessionStorage.setItem(AUTH_KEY, 'true')
export const clearAuthentication = () => window.sessionStorage.removeItem(AUTH_KEY)
