import { useAuthStore, type AuthUser } from '../store/authStore'
import { useCartStore } from '../store/cartStore'

export function resolveApiBase(raw: string | undefined): string {
  const value = (raw ?? '').trim().replace(/\/+$/, '')
  if (!value) return '/api'
  if (value === '/api' || value.endsWith('/api')) return value
  return `${value}/api`
}

const API_BASE = resolveApiBase(import.meta.env.VITE_API_BASE)

export function joinApiPath(path: string, base = API_BASE): string {
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const prefix = base.replace(/\/+$/, '')
  const suffix = path.startsWith('/') ? path : `/${path}`
  return `${prefix}${suffix}`
}

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
  avatar?: string | null
}

type MessageResponse = {
  message: string
}

let refreshPromise: Promise<string | null> | null = null

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
  if (response.status === 405) {
    return new ApiError(
      'This static host does not accept API writes. Use the local Vite app (proxy /api → gateway) or deploy the Azure gateway and set VITE_API_BASE.',
      405
    )
  }
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

  if (response.status === 401 && options.retryOn401 !== false && !path.startsWith('/auth/')) {
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

async function mergeGuestCart(): Promise<void> {
  const guestToken = useCartStore.getState().guestToken
  if (!guestToken) return
  try {
    const cart = await apiRequest<{ items: { productId: string; qty: number }[] }>(
      `/cart/merge?guestToken=${encodeURIComponent(guestToken)}`,
      { method: 'POST' }
    )
    useCartStore.getState().setItems(cart.items ?? [])
  } catch {
    // Session is already established; cart merge is best-effort.
  }
}

async function establishSession(auth: AuthResponse): Promise<AuthUser> {
  const user = mapAuthUser(auth)
  useAuthStore.getState().setSession(auth.accessToken, user)
  await mergeGuestCart()
  return user
}

export const api = {
  get: <T>(path: string) => apiRequest<T>(path),
  post: <T>(path: string, body?: unknown, includeAuth = true) =>
    apiRequest<T>(path, { method: 'POST', body, includeAuth }),
  patch: <T>(path: string, body: unknown) => apiRequest<T>(path, { method: 'PATCH', body })
}

export const authApi = {
  login: async (payload: { email: string; password: string }): Promise<AuthUser> => {
    const auth = await apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: payload,
      includeAuth: false,
      retryOn401: false
    })
    return establishSession(auth)
  },
  register: async (payload: { name: string; email: string; password: string }): Promise<AuthUser> => {
    const auth = await apiRequest<AuthResponse>('/auth/register', {
      method: 'POST',
      body: payload,
      includeAuth: false,
      retryOn401: false
    })
    return establishSession(auth)
  },
  google: async (idToken: string): Promise<AuthUser> => {
    const auth = await apiRequest<AuthResponse>('/auth/google', {
      method: 'POST',
      body: { idToken },
      includeAuth: false,
      retryOn401: false
    })
    return establishSession(auth)
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
    try {
      await apiRequest<void>('/auth/logout', { method: 'POST', retryOn401: false })
    } catch {
      // Server revoke is best-effort; the browser session must still end.
    } finally {
      useAuthStore.getState().clearSession()
    }
  },
  bootstrapSession: async (): Promise<void> => {
    try {
      const refreshedToken = await refreshAccessToken()
      if (!refreshedToken) return
      await authApi.me()
      await mergeGuestCart()
    } catch {
      useAuthStore.getState().clearSession()
    } finally {
      useAuthStore.getState().setBootstrapped(true)
    }
  }
}
