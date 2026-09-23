import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import RegisterPage from './RegisterPage'
import { useAuthStore } from '../store/authStore'

function renderRegister() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/shop" element={<p>Shop Page</p>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('RegisterPage', () => {
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

  it('checks the name, password, and confirmation before creating a customer', async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (String(url).includes('/auth/register')) {
        return new Response(JSON.stringify({
          accessToken: 'jwt',
          userId: 'u1',
          email: 'ada@eco-organic-store.com',
          name: 'Ada Lovelace',
          roles: ['CUSTOMER'],
          avatar: null
        }), { status: 200, headers: { 'Content-Type': 'application/json' } })
      }
      return new Response(JSON.stringify({ items: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    })
    vi.stubGlobal('fetch', fetchMock)
    renderRegister()

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect((await screen.findByRole('alert')).textContent).toContain('Enter your full name.')
    expect(fetchMock).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Ada Lovelace' } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ada@eco-organic-store.com' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret123' } })
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'different' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect((await screen.findByRole('alert')).textContent).toContain('Passwords do not match.')

    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'secret123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('Shop Page')).toBeTruthy()

    const registerCall = fetchMock.mock.calls.find((call) => String(call[0]).includes('/auth/register'))
    const body = JSON.parse(String((registerCall?.[1] as RequestInit).body))
    expect(body).toEqual({ name: 'Ada Lovelace', email: 'ada@eco-organic-store.com', password: 'secret123' })
    expect(body.roles).toBeUndefined()
  })
})
