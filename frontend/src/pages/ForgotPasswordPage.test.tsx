import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ForgotPasswordPage from './ForgotPasswordPage'

function renderPage(path = '/forgot-password') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ForgotPasswordPage />
    </MemoryRouter>
  )
}

describe('ForgotPasswordPage', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('asks for an email and does not reveal whether the account exists', async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify({
      message: 'If an account exists, password reset instructions will be sent.'
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    renderPage()

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ada@eco-organic-store.com' } })
    fireEvent.click(screen.getByRole('button', { name: 'Email me a reset link' }))

    expect(await screen.findByRole('status')).toBeTruthy()
    expect(screen.getByRole('status').textContent).toContain('If an account exists')
    expect(screen.queryByLabelText('Reset token')).toBeNull()
  })

  it('uses the token from the email link to set a password', async () => {
    const fetchMock = vi.fn(async (_url: string, _init?: RequestInit) => new Response(JSON.stringify({
      message: 'Password updated. Sign in with your new password.'
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)
    renderPage('/forgot-password?token=reset-token')

    expect(screen.queryByLabelText('Email')).toBeNull()
    fireEvent.change(screen.getByLabelText('New password'), { target: { value: 'new-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Set new password' }))

    await waitFor(() => {
      const body = fetchMock.mock.calls[0]?.[1]?.body
      expect(body).toBeTypeOf('string')
      expect(JSON.parse(String(body))).toEqual({ token: 'reset-token', newPassword: 'new-password' })
    })
    expect(await screen.findByRole('status')).toBeTruthy()
  })
})
