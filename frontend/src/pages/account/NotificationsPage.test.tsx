import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import NotificationsPage from './NotificationsPage'

const preferences = {
  orderUpdates: true,
  email: 'asha@eco-organic-store.com',
  fromAddress: 'admin@eco-organic-store.com'
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <NotificationsPage />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('NotificationsPage', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('saves the order-update choice', async () => {
    const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'PUT') {
        return new Response(JSON.stringify({ ...preferences, orderUpdates: false }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        })
      }
      return new Response(JSON.stringify(preferences), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    })
    vi.stubGlobal('fetch', fetchMock)
    renderPage()

    const toggle = await screen.findByRole('checkbox', { name: /Order updates/ })
    expect((toggle as HTMLInputElement).checked).toBe(true)
    fireEvent.click(toggle)
    fireEvent.click(screen.getByRole('button', { name: 'Save email preferences' }))

    await waitFor(() => {
      const put = fetchMock.mock.calls.find((call) => (call[1] as RequestInit | undefined)?.method === 'PUT')
      const init = put?.[1] as RequestInit | undefined
      expect(init?.body).toBeTypeOf('string')
      expect(JSON.parse(String(init?.body))).toEqual({ orderUpdates: false })
      expect(String(put?.[0])).toContain('/me/notifications')
    })
    expect(await screen.findByRole('status')).toBeTruthy()
  })
})
