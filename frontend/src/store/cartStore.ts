import { create } from 'zustand'

export type CartItem = { productId: string; qty: number }

type CartState = {
  guestToken: string
  items: CartItem[]
  setItems: (items: CartItem[]) => void
}

function readGuestToken(): string {
  if (typeof window === 'undefined') return crypto.randomUUID()
  const existing = localStorage.getItem('guestToken')
  if (existing) return existing
  const token = crypto.randomUUID()
  localStorage.setItem('guestToken', token)
  return token
}

export const useCartStore = create<CartState>((set) => ({
  guestToken: readGuestToken(),
  items: [],
  setItems: (items) => set({ items })
}))
