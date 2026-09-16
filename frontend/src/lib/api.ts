import { useAuthStore, type AuthUser } from '../store/authStore'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

type AuthResponse = {
  accessToken: string
  userId: string
  email: string
  name: string
  roles: string[]
}

type MessageResponse = {
  message: string
}

let refreshPromise: Promise<string | null> | null = null

function joinApiPath(path: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`
}

function mapAuthUser(payload: { userId: string; email: string; name: string; roles: string[]; avatar?: string | null }): AuthUser {
  return {
    userId: payload.userId,
    email: payload.email,
    name: payload.name,
    roles: payload.roles ?? [],
    avatar: payload.avatar ?? null
  }
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    const body = await response.json()
    const detail = typeof body?.detail === 'string' ? body.detail : `Request failed (${response.status})`
    return new ApiError(detail, response.status)
  } catch {
    return new ApiError(`Request failed (${response.status})`, response.status)
  }
}

async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const response = await fetch(joinApiPath('/auth/refresh'), {
        method: 'POST',
        credentials: 'include'
      })
      if (!response.ok) {
        useAuthStore.getState().clearSession()
        return null
      }
      const payload = (await response.json()) as AuthResponse
      useAuthStore.getState().setAccessToken(payload.accessToken)
      return payload.accessToken
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

type RequestOptions = {
  method?: string
  body?: unknown
  retryOn401?: boolean
  includeAuth?: boolean
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const token = useAuthStore.getState().accessToken
  const headers = new Headers({ Accept: 'application/json' })
  const method = options.method ?? 'GET'
  const includeAuth = options.includeAuth ?? true

  if (options.body !== undefined) headers.set('Content-Type', 'application/json')
  if (includeAuth && token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(joinApiPath(path), {
    method,
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    credentials: 'include'
  })

  if (response.status === 401 && options.retryOn401 !== false && !path.startsWith('/auth/refresh')) {
    const refreshedToken = await refreshAccessToken()
    if (refreshedToken) {
      return apiRequest<T>(path, { ...options, retryOn401: false })
    }
    throw new ApiError('Session expired. Please login again.', 401)
  }

  if (!response.ok) throw await parseError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const api = {
  get: <T>(path: string) => apiRequest<T>(path),
  post: <T>(path: string, body?: unknown, includeAuth = true) =>
    apiRequest<T>(path, { method: 'POST', body, includeAuth }),
  patch: <T>(path: string, body: unknown) => apiRequest<T>(path, { method: 'PATCH', body })
}

export const authApi = {
  login: async (payload: { email: string; password: string }): Promise<AuthUser> => {
    const auth = await api.post<AuthResponse>('/auth/login', payload, false)
    const user = mapAuthUser(auth)
    useAuthStore.getState().setSession(auth.accessToken, user)
    return user
  },
  register: async (payload: { name: string; email: string; password: string }): Promise<AuthUser> => {
    const auth = await api.post<AuthResponse>('/auth/register', payload, false)
    const user = mapAuthUser(auth)
    useAuthStore.getState().setSession(auth.accessToken, user)
    return user
  },
  google: async (idToken: string): Promise<AuthUser> => {
    const auth = await api.post<AuthResponse>('/auth/google', { idToken }, false)
    const user = mapAuthUser(auth)
    useAuthStore.getState().setSession(auth.accessToken, user)
    return user
  },
  requestReset: (email: string) => api.post<MessageResponse>('/auth/request-reset', { email }, false),
  confirmReset: (token: string, newPassword: string) =>
    api.post<MessageResponse>('/auth/confirm-reset', { token, newPassword }, false),
  me: async (): Promise<AuthUser> => {
    const profile = await api.get<AuthUser>('/me')
    const user = mapAuthUser(profile)
    useAuthStore.getState().setUser(user)
    return user
  },
  logout: async (): Promise<void> => {
    await api.post<void>('/auth/logout')
    useAuthStore.getState().clearSession()
  },
  bootstrapSession: async (): Promise<void> => {
    try {
      const refreshedToken = await refreshAccessToken()
      if (!refreshedToken) return
      await authApi.me()
    } catch {
      useAuthStore.getState().clearSession()
    } finally {
      useAuthStore.getState().setBootstrapped(true)
    }
  }
}
