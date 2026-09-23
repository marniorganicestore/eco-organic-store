import { create } from 'zustand'

export type CartLine = {
  productId: string
  qty: number
  slug: string
  name: string
  unit: string
  origin: string
  image: string | null
  pricePaise: number
  lineTotalPaise: number
  available: number | null
  purchasable: boolean
}

type NoticeTone = 'success' | 'error'

type CartState = {
  guestToken: string
  items: CartLine[]
  acceptingCart: boolean
  notice: string | null
  noticeTone: NoticeTone
  setItems: (items: CartLine[]) => void
  pauseForMerge: () => void
  resumeCart: () => void
  flash: (message: string, tone?: NoticeTone) => void
}

let noticeTimer: number | undefined

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
  acceptingCart: true,
  notice: null,
  noticeTone: 'success',
  setItems: (items) => set({ items }),
  pauseForMerge: () => set({ acceptingCart: false }),
  resumeCart: () => set({ acceptingCart: true }),
  flash: (message, tone = 'success') => {
    if (typeof window !== 'undefined') {
      window.clearTimeout(noticeTimer)
      noticeTimer = window.setTimeout(() => set({ notice: null }), 3600)
    }
    set({ notice: message, noticeTone: tone })
  }
}))
