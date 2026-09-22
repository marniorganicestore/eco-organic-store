import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import LoginPage from './LoginPage'
import { useAuthStore } from '../store/authStore'

function renderLogin(from?: string) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/login', state: from ? { from } : null }]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/shop" element={<p>Shop Page</p>} />
        <Route path="/checkout" element={<p>Checkout Page</p>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    cleanup()
    localStorage.clear()
    useAuthStore.getState().clearSession()
    useAuthStore.getState().setBootstrapped(true)
    vi.unstubAllGlobals()
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('requires a valid email and password before calling the API', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    renderLogin()

    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByRole('alert').textContent).toContain('Enter a valid email address.')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('signs in and returns to the requested page', async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (String(url).includes('/auth/login')) {
        return new Response(JSON.stringify({
          accessToken: 'jwt',
          userId: 'u1',
          email: 'user@harvest.co',
          name: 'Asha Farmer',
          roles: ['CUSTOMER'],
          avatar: null
        }), { status: 200, headers: { 'Content-Type': 'application/json' } })
      }
      if (String(url).includes('/cart/merge')) {
        return new Response(JSON.stringify({ items: [] }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        })
      }
      return new Response('missing', { status: 404 })
    })
    vi.stubGlobal('fetch', fetchMock)

    renderLogin('/checkout')
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@harvest.co' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Checkout Page')).toBeTruthy()
    expect(useAuthStore.getState().accessToken).toBe('jwt')
    expect(useAuthStore.getState().user?.name).toBe('Asha Farmer')
    expect(fetchMock.mock.calls.some((call) => String(call[0]).includes('/auth/login'))).toBe(true)
    expect(fetchMock.mock.calls.some((call) => String(call[0]).includes('/cart/merge'))).toBe(true)
  })

  it('shows a generic message for invalid credentials and does not refresh', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'Invalid email or password' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' }
    }))
    vi.stubGlobal('fetch', fetchMock)

    renderLogin()
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@harvest.co' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrongpass' } })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByRole('alert').textContent).toContain('Invalid email or password.')
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe('/api/auth/login')
  })
})
