import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import ProfilePage from './ProfilePage'
import { useAuthStore } from '../../store/authStore'

const profile = {
  userId: 'u1',
  email: 'asha@harvest.co',
  name: 'Asha Rao',
  avatar: null,
  phone: '9876543210',
  roles: ['CUSTOMER'],
  passwordSet: true,
  googleLinked: false,
  addresses: []
}

function renderProfile() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('ProfilePage', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    useAuthStore.getState().clearSession()
  })

  it('shows the account and blocks a too-short name before saving', async () => {
    useAuthStore.getState().setSession('token', {
      userId: 'u1',
      email: 'asha@harvest.co',
      name: 'Asha Rao',
      roles: ['CUSTOMER'],
      avatar: null
    })
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(profile), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    renderProfile()

    expect(await screen.findByRole('heading', { name: 'Asha Rao' })).toBeTruthy()
    expect(screen.getByText(/is your sign-in address/)).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'A' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))

    expect(await screen.findByRole('alert')).toBeTruthy()
    expect(screen.getByRole('alert').textContent).toContain('Name must be 2–80 characters.')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})