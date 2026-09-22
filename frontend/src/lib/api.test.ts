import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi, joinApiPath, resolveApiBase } from './api'
import { useAuthStore } from '../store/authStore'

describe('resolveApiBase', () => {
  it('defaults blank values to the Vite/gateway proxy prefix', () => {
    expect(resolveApiBase(undefined)).toBe('/api')
    expect(resolveApiBase('')).toBe('/api')
    expect(resolveApiBase('   ')).toBe('/api')
  })

  it('keeps an explicit /api prefix and appends it to a host', () => {
    expect(resolveApiBase('/api')).toBe('/api')
    expect(resolveApiBase('https://api.harvest.test/api/')).toBe('https://api.harvest.test/api')
    expect(resolveApiBase('https://api.harvest.test')).toBe('https://api.harvest.test/api')
  })
})

describe('joinApiPath', () => {
  it('builds auth URLs under /api so static hosts are not POSTed at /auth/register', () => {
    expect(joinApiPath('/auth/register')).toBe('/api/auth/register')
    expect(joinApiPath('/auth/register', '/api')).toBe('/api/auth/register')
    expect(joinApiPath('auth/login', '/api')).toBe('/api/auth/login')
    expect(joinApiPath('/auth/register', 'https://api.harvest.test/api')).toBe(
      'https://api.harvest.test/api/auth/register'
    )
  })
})

describe('authApi.logout', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().setSession('access-token', {
      userId: 'u1',
      email: 'user@harvest.co',
      name: 'User',
      roles: ['CUSTOMER']
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('posts logout with the access token and then clears the session', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await authApi.logout()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/auth/logout')
    expect(init.method).toBe('POST')
    expect(init.credentials).toBe('include')
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer access-token')
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
    expect(localStorage.getItem('harvest.accessToken')).toBeNull()
  })

  it('clears the session even when logout fails and does not attempt refresh', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'expired' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    }))
    vi.stubGlobal('fetch', fetchMock)

    await authApi.logout()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/auth/logout')
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(localStorage.getItem('harvest.user')).toBeNull()
  })
})
