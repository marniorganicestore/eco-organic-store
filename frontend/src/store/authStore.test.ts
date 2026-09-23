import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './authStore'

describe('authStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.getState().clearSession()
    useAuthStore.getState().setBootstrapped(false)
  })

  it('stores and clears session data', () => {
    useAuthStore.getState().setSession('token-1', {
      userId: 'u1',
      email: 'user@eco-organic-store.com',
      name: 'User',
      roles: ['CUSTOMER']
    })

    expect(useAuthStore.getState().accessToken).toBe('token-1')
    expect(useAuthStore.getState().user?.email).toBe('user@eco-organic-store.com')
    expect(localStorage.getItem('eco.accessToken')).toBe('token-1')

    useAuthStore.getState().clearSession()

    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
    expect(localStorage.getItem('eco.accessToken')).toBeNull()
  })
})
