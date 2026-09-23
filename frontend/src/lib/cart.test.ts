import { describe, expect, it } from 'vitest'
import { checkoutBlocker, formatInr, lineNote, withQuantity, type CartView } from './cart'
import type { CartLine } from '../store/cartStore'

function line(overrides: Partial<CartLine> = {}): CartLine {
  return {
    productId: 'p1',
    qty: 1,
    slug: 'baby-spinach',
    name: 'Baby Spinach',
    unit: '250 g',
    origin: 'Nilgiris',
    image: null,
    pricePaise: 17900,
    lineTotalPaise: 17900,
    available: 8,
    purchasable: true,
    ...overrides
  }
}

function basket(items: CartLine[]): CartView {
  return {
    items,
    itemCount: items.reduce((sum, item) => sum + item.qty, 0),
    subtotalPaise: items.reduce((sum, item) => sum + (item.purchasable ? item.lineTotalPaise : 0), 0)
  }
}

describe('cart view', () => {
  it('formats rupees from paise', () => {
    expect(formatInr(17900)).toBe('₹179.00')
  })

  it('explains stock and dropped products', () => {
    expect(lineNote(line({ available: 3, qty: 1 }))).toBe('Only 3 left.')
    expect(lineNote(line({ available: 2, qty: 4, purchasable: false }))).toBe('Only 2 available.')
    expect(lineNote(line({ available: 0, qty: 1, purchasable: false }))).toBe('Out of stock.')
    expect(lineNote(line({ slug: '', name: 'No longer available', purchasable: false, pricePaise: 0, lineTotalPaise: 0 }))).toBe('This item is no longer sold.')
  })

  it('blocks checkout until every line can be bought', () => {
    expect(checkoutBlocker(basket([line()]))).toBeNull()
    expect(checkoutBlocker(basket([line({ qty: 4, available: 2, purchasable: false })]))).toBe('Reduce quantities to what is in stock.')
    expect(checkoutBlocker(basket([]))).toBe('Your basket is empty.')
  })

  it('recalculates the line and drops it when the quantity is zero', () => {
    const cart = basket([line({ qty: 2, lineTotalPaise: 35800 })])
    expect(withQuantity(cart, 'p1', 3).subtotalPaise).toBe(53700)
    expect(withQuantity(cart, 'p1', 0).items).toEqual([])
    expect(withQuantity(cart, 'p1', 0).subtotalPaise).toBe(0)
  })
})
