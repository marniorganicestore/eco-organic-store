import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AdminUsersPanel } from './AdminUsersPanel'
import { useAuthStore } from '../../store/authStore'

const accounts = [
  {
    userId: 'admin-1',
    email: 'admin@eco-organic-store.com',
    name: 'Store Admin',
    avatar: null,
    roles: ['CUSTOMER', 'ADMIN'],
    enabled: true
  },
  {
    userId: 'u2',
    email: 'ada@eco-organic-store.com',
    name: 'Ada Lovelace',
    avatar: null,
    roles: ['CUSTOMER'],
    enabled: true
  }
]

function renderPanel() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <AdminUsersPanel />
    </QueryClientProvider>
  )
}

describe('AdminUsersPanel', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    useAuthStore.getState().clearSession()
  })

  it('grants admin without letting the signed-in admin demote themselves', async () => {
    useAuthStore.getState().setSession('token', {
      userId: 'admin-1',
      email: 'admin@eco-organic-store.com',
      name: 'Store Admin',
      roles: ['CUSTOMER', 'ADMIN']
    })
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (String(url).includes('/admin/users/u2') && init?.method === 'PATCH') {
        return new Response(JSON.stringify({
          ...accounts[1],
          roles: ['CUSTOMER', 'ADMIN']
        }), { status: 200, headers: { 'Content-Type': 'application/json' } })
      }
      return new Response(JSON.stringify({
        items: accounts,
        page: 0,
        size: 20,
        totalElements: accounts.length,
        totalPages: 1,
        hasNext: false
      }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    })
    vi.stubGlobal('fetch', fetchMock)
    renderPanel()

    expect(await screen.findByText('Ada Lovelace')).toBeTruthy()
    const selfCustomer = screen.getByRole('button', { name: 'Customer', pressed: false })
    expect(selfCustomer).toHaveProperty('disabled', true)

    const grant = screen.getAllByRole('button', { name: 'Admin', pressed: false })[0]
    fireEvent.click(grant)
    await waitFor(() => {
      expect(screen.getByText('Ada Lovelace').closest('li')?.textContent).toContain('Customer · Admin · Active')
    })
    const patch = fetchMock.mock.calls.find((call) => String(call[0]).includes('/admin/users/u2'))
    expect(JSON.parse(String((patch?.[1] as RequestInit).body))).toEqual({
      roles: ['CUSTOMER', 'ADMIN']
    })
  })
})
