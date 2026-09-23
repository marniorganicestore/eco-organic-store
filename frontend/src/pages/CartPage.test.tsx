import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import CartPage from './CartPage'
import { useAuthStore } from '../store/authStore'
import { useCartStore, type CartLine } from '../store/cartStore'

function spinach(qty = 1): CartLine {
  return {
    productId: 'p1',
    qty,
    slug: 'baby-spinach',
    name: 'Baby Spinach',
    unit: '250 g',
    origin: 'Nilgiris',
    image: '/images/spinach.jpg',
    pricePaise: 17900,
    lineTotalPaise: 17900 * qty,
    available: 8,
    purchasable: true
  }
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

function renderCart() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <CartPage />
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('CartPage', () => {
  beforeEach(() => {
    cleanup()
    localStorage.clear()
    useAuthStore.getState().clearSession()
    useAuthStore.getState().setBootstrapped(true)
    useCartStore.setState({ guestToken: 'guest-1', items: [], acceptingCart: true, notice: null })
    vi.unstubAllGlobals()
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows the product, price, and a way to change quantity', async () => {
    const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'PATCH') return json({ items: [spinach(2)], itemCount: 2, subtotalPaise: 35800 })
      return json({ items: [spinach(1)], itemCount: 1, subtotalPaise: 17900 })
    })
    vi.stubGlobal('fetch', fetchMock)

    renderCart()

    expect(await screen.findByRole('link', { name: 'Baby Spinach' })).toBeTruthy()
    expect(screen.getByText('250 g · Nilgiris')).toBeTruthy()
    expect(screen.getByText('₹179.00 each')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Proceed to checkout' })).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Increase quantity of Baby Spinach' }))

    await waitFor(() => {
      const patch = fetchMock.mock.calls.find((call) => call[1]?.method === 'PATCH')
      expect(patch?.[1]?.body).toBe(JSON.stringify({ productId: 'p1', qty: 2 }))
    })
  })

  it('offers the shop when the basket is empty', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => json({ items: [], itemCount: 0, subtotalPaise: 0 })))
    renderCart()
    expect(await screen.findByRole('heading', { name: 'Your basket is empty' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Shop organic produce' })).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Proceed to checkout' })).toBeNull()
  })

  it('keeps checkout closed when an item is no longer sold', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => json({
      items: [{
        ...spinach(1),
        slug: '',
        name: 'No longer available',
        pricePaise: 0,
        lineTotalPaise: 0,
        available: null,
        purchasable: false
      }],
      itemCount: 1,
      subtotalPaise: 0
    })))
    renderCart()
    expect(await screen.findByText('This item is no longer sold.')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Proceed to checkout' }).hasAttribute('disabled')).toBe(true)
    expect(screen.getByRole('button', { name: 'Remove No longer available' })).toBeTruthy()
  })
})
