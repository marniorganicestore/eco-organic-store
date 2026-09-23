import { create } from 'zustand'

export type AuthUser = {
  userId: string
  email: string
  name: string
  avatar?: string | null
  roles: string[]
}

type AuthState = {
  accessToken: string | null
  user: AuthUser | null
  bootstrapped: boolean
  setSession: (token: string, user: AuthUser) => void
  setAccessToken: (token: string | null) => void
  setUser: (user: AuthUser | null) => void
  setBootstrapped: (value: boolean) => void
  clearSession: () => void
}

const TOKEN_KEY = 'eco.accessToken'
const USER_KEY = 'eco.user'

function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(TOKEN_KEY)
}

function getStoredUser(): AuthUser | null {
  if (typeof window === 'undefined') return null
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: getStoredToken(),
  user: getStoredUser(),
  bootstrapped: false,
  setSession: (token, user) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    }
    set({ accessToken: token, user })
  },
  setAccessToken: (token) => {
    if (typeof window !== 'undefined') {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    }
    set({ accessToken: token })
  },
  setUser: (user) => {
    if (typeof window !== 'undefined') {
      if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
      else localStorage.removeItem(USER_KEY)
    }
    set({ user })
  },
  setBootstrapped: (value) => set({ bootstrapped: value }),
  clearSession: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
    set({ accessToken: null, user: null })
  }
}))
