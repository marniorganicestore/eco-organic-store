export type RazorpaySuccess = {
  razorpay_payment_id: string
  razorpay_order_id: string
  razorpay_signature: string
}

type RazorpayFailure = {
  error?: {
    description?: string
  }
}

type RazorpayCheckout = {
  open: () => void
  on: (event: 'payment.failed', handler: (response: RazorpayFailure) => void) => void
}

type RazorpayOptions = {
  key: string
  amount: number
  currency: string
  name: string
  description: string
  order_id: string
  prefill?: {
    name?: string
    email?: string
  }
  theme?: {
    color: string
  }
  handler: (response: RazorpaySuccess) => void
  modal: {
    ondismiss: () => void
  }
}

type RazorpayConstructor = new (options: RazorpayOptions) => RazorpayCheckout

declare global {
  interface Window {
    Razorpay?: RazorpayConstructor
  }
}

export function razorpayKeyId(serverKeyId: string | undefined): string {
  const fromServer = serverKeyId?.trim() ?? ''
  if (fromServer) return fromServer
  return import.meta.env.VITE_RAZORPAY_KEY_ID?.trim() ?? ''
}

export function openRazorpayCheckout(options: {
  keyId: string
  orderId: string
  amount: number
  currency: string
  description: string
  name?: string
  email?: string
  onSuccess: (payload: RazorpaySuccess) => void
  onDismiss: () => void
  onFailure: (message: string) => void
}): void {
  const Razorpay = window.Razorpay
  if (!Razorpay) {
    options.onFailure('Payment checkout failed to load. Refresh the page and try again.')
    return
  }
  let settled = false
  const checkout = new Razorpay({
    key: options.keyId,
    amount: options.amount,
    currency: options.currency,
    name: 'Marni Eco organic store',
    description: options.description,
    order_id: options.orderId,
    prefill: {
      name: options.name,
      email: options.email
    },
    theme: { color: '#047857' },
    handler(response) {
      settled = true
      options.onSuccess(response)
    },
    modal: {
      ondismiss() {
        if (!settled) options.onDismiss()
      }
    }
  })
  checkout.on('payment.failed', (response) => {
    settled = true
    options.onFailure(response.error?.description?.trim() || 'Payment failed. Nothing was charged.')
  })
  checkout.open()
}
