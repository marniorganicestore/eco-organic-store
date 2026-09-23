import { beforeEach, describe, expect, it } from 'vitest'
import { useCartStore, type CartLine } from './cartStore'

const spinach: CartLine = {
  productId: 'p1',
  qty: 2,
  slug: 'baby-spinach',
  name: 'Baby Spinach',
  unit: '250 g',
  origin: 'Nilgiris',
  image: null,
  pricePaise: 17900,
  lineTotalPaise: 35800,
  available: 8,
  purchasable: true
}

describe('cart store', () => {
  beforeEach(() => {
    localStorage.clear()
    useCartStore.setState({ items: [], notice: null, acceptingCart: true })
  })

  it('replaces the basket the header counts', () => {
    useCartStore.getState().setItems([spinach])
    expect(useCartStore.getState().items.reduce((sum, item) => sum + item.qty, 0)).toBe(2)
    useCartStore.getState().setItems([])
    expect(useCartStore.getState().items).toEqual([])
  })

  it('pauses basket reads while a guest cart is merging', () => {
    useCartStore.getState().pauseForMerge()
    expect(useCartStore.getState().acceptingCart).toBe(false)
    useCartStore.getState().resumeCart()
    expect(useCartStore.getState().acceptingCart).toBe(true)
  })
})
