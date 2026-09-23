import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AdminOrdersPage from './AdminOrdersPage'

const orders = [
  {
    id: 'o1',
    orderNumber: 'ECO-1001',
    userId: 'u1',
    lines: [{ productId: 'p1', productName: 'Organic Baby Spinach', pricePaise: 17900, qty: 1 }],
    shippingAddress: '12 Farm Road, Mysuru',
    totalPaise: 17900,
    orderStatus: 'CONFIRMED',
    createdAt: '2026-09-23T08:00:00Z'
  },
  {
    id: 'o2',
    orderNumber: 'ECO-1002',
    userId: 'u2',
    lines: [],
    shippingAddress: 'Waiting',
    totalPaise: 9900,
    orderStatus: 'PENDING_PAYMENT',
    createdAt: '2026-09-23T09:00:00Z'
  }
]

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <AdminOrdersPage />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('AdminOrdersPage', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('offers the next fulfillment step and leaves unpaid orders alone', async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (String(url).includes('/admin/orders/ECO-1001') && init?.method === 'PATCH') {
        return new Response(JSON.stringify({ ...orders[0], orderStatus: 'PACKED' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        })
      }
      return new Response(JSON.stringify(orders), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      })
    })
    vi.stubGlobal('fetch', fetchMock)
    renderPage()

    expect(await screen.findByRole('button', { name: 'Mark packed' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Mark shipped' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Mark packed' }))

    await waitFor(() => {
      const patch = fetchMock.mock.calls.find((call) => String(call[0]).includes('/admin/orders/ECO-1001'))
      const init = patch?.[1] as RequestInit | undefined
      expect(init?.body).toBeTypeOf('string')
      expect(JSON.parse(String(init?.body))).toEqual({ status: 'PACKED' })
    })
    expect(await screen.findByText('Packed')).toBeTruthy()
  })
})
