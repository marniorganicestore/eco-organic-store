import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { RequireAdmin, RequireAuth } from './RequireAuth'
import { useAuthStore } from '../../store/authStore'

function renderAuthTree(path: string, element: ReactNode) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<p>Login Page</p>} />
        <Route path="/shop" element={<p>Shop Page</p>} />
        <Route path="*" element={element} />
      </Routes>
    </MemoryRouter>
  )
}

describe('route guards', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
    useAuthStore.getState().setBootstrapped(true)
  })

  it('redirects anonymous users to login', () => {
    renderAuthTree('/checkout', (
      <RequireAuth>
        <p>Checkout Page</p>
      </RequireAuth>
    ))

    expect(screen.getByText('Login Page')).toBeTruthy()
  })

  it('redirects non-admin users from admin pages', () => {
    useAuthStore.getState().setSession('token', {
      userId: 'u1',
      email: 'user@eco-organic-store.com',
      name: 'User',
      roles: ['CUSTOMER']
    })

    renderAuthTree('/admin', (
      <RequireAdmin>
        <p>Admin Page</p>
      </RequireAdmin>
    ))

    expect(screen.getByText('Shop Page')).toBeTruthy()
  })
})
