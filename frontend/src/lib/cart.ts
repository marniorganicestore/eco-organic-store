import { queryClient } from './queryClient'
import { useCartStore, type CartLine } from '../store/cartStore'

export const MAX_CART_QTY = 24

export type CartView = {
  items: CartLine[]
  itemCount: number
  subtotalPaise: number
}

export function cartQueryKey(scope: string) {
  return ['cart', scope] as const
}

export function cartCollectionPath(guestToken: string): string {
  return `/cart?guestToken=${encodeURIComponent(guestToken)}`
}

export function cartMergePath(guestToken: string): string {
  return `/cart/merge?guestToken=${encodeURIComponent(guestToken)}`
}

export function formatInr(paise: number): string {
  return `₹${(paise / 100).toFixed(2)}`
}

export function lineNote(line: CartLine): string | null {
  if (!line.slug) return 'This item is no longer sold.'
  if (!line.purchasable && (line.available == null || line.available > line.qty)) return 'This item is no longer sold.'
  if (line.available === 0) return 'Out of stock.'
  if (line.available != null && line.qty > line.available) return `Only ${line.available} available.`
  if (line.available != null && line.available > 0 && line.available <= 5) return `Only ${line.available} left.`
  return null
}

export function checkoutBlocker(cart: CartView): string | null {
  if (cart.items.length === 0) return 'Your basket is empty.'
  if (cart.items.some((line) => line.available != null && line.qty > line.available)) {
    return 'Reduce quantities to what is in stock.'
  }
  if (cart.items.some((line) => line.available === 0)) return 'Some items are out of stock.'
  if (cart.items.some((line) => !line.purchasable)) return 'Remove items that are no longer sold.'
  return null
}

export function withQuantity(cart: CartView, productId: string, qty: number): CartView {
  const items = qty <= 0
    ? cart.items.filter((line) => line.productId !== productId)
    : cart.items.map((line) => (line.productId === productId ? retotal(line, qty) : line))
  return summarize(items)
}

export function rememberCart(cart: CartView, scope: string) {
  useCartStore.getState().setItems(cart.items ?? [])
  queryClient.setQueryData(cartQueryKey(scope), cart)
}

function retotal(line: CartLine, qty: number): CartLine {
  const purchasable = Boolean(line.slug) && (line.available == null || (line.available > 0 && qty <= line.available))
  return {
    ...line,
    qty,
    lineTotalPaise: line.pricePaise * qty,
    purchasable
  }
}

function summarize(items: CartLine[]): CartView {
  return {
    items,
    itemCount: items.reduce((sum, line) => sum + line.qty, 0),
    subtotalPaise: items.reduce((sum, line) => sum + (line.purchasable ? line.lineTotalPaise : 0), 0)
  }
}
