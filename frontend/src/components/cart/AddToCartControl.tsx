import { useState } from 'react'
import { ApiError } from '../../lib/api'
import { MAX_CART_QTY } from '../../lib/cart'
import { storeBtn } from '../layout/PageShell'
import { FormBanner } from '../account/FormBanner'
import { useCartStore } from '../../store/cartStore'
import { useAddToCart } from '../../hooks/useCart'
import { QtyStepper } from './QtyStepper'

type AddToCartControlProps = {
  productId: string
  productName: string
  layout: 'card' | 'detail'
}

function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Unable to add this item.'
}

export function AddToCartControl({ productId, productName, layout }: AddToCartControlProps) {
  const addToCart = useAddToCart()
  const inBasket = useCartStore((state) => state.items.find((item) => item.productId === productId)?.qty ?? 0)
  const [qty, setQty] = useState(1)
  const room = Math.max(0, MAX_CART_QTY - inBasket)
  const pending = addToCart.isPending

  async function add(amount: number) {
    if (room < 1 || amount < 1) return
    try {
      await addToCart.mutateAsync({ productId, qty: Math.min(amount, room) })
    } catch {
      // The mutation records the message for the alert under this control.
    }
  }

  if (layout === 'card') {
    return (
      <div>
        <button
          type="button"
          onClick={() => add(1)}
          disabled={pending || room < 1}
          className={`${storeBtn} mt-3 w-full`}
        >
          {pending ? 'Adding...' : room < 1 ? 'Basket full' : 'Add to cart'}
        </button>
        {addToCart.isError ? <div className="mt-2"><FormBanner tone="error">{errorMessage(addToCart.error)}</FormBanner></div> : null}
      </div>
    )
  }

  const stepperMax = Math.max(room, 1)

  return (
    <div className="space-y-3">
      {inBasket > 0 ? <p className="text-sm text-emerald-800">{inBasket} already in your basket.</p> : null}
      <QtyStepper
        qty={Math.min(qty, stepperMax)}
        min={1}
        max={stepperMax}
        label={productName}
        disabled={pending || room < 1}
        onChange={setQty}
      />
      <button
        type="button"
        onClick={() => add(qty)}
        disabled={pending || room < 1}
        className={`${storeBtn} w-full`}
      >
        {pending ? 'Adding...' : room < 1 ? 'Basket is full for this item' : 'Add to cart'}
      </button>
      {addToCart.isError ? <FormBanner tone="error">{errorMessage(addToCart.error)}</FormBanner> : null}
    </div>
  )
}
