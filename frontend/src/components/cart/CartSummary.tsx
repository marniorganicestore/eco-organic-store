import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { checkoutBlocker, formatInr, type CartView } from '../../lib/cart'
import { storeBtn, storeBtnGhost, storeCard } from '../layout/PageShell'

type CartSummaryProps = {
  cart?: CartView
  loading?: boolean
  showLines?: boolean
  showCheckout?: boolean
  action?: ReactNode
}

export function CartSummary({ cart, loading = false, showLines = false, showCheckout = true, action }: CartSummaryProps) {
  if (loading || !cart) {
    return <div className={`${storeCard} h-48 animate-pulse bg-emerald-100/70`} aria-hidden="true" />
  }

  const blocker = checkoutBlocker(cart)
  const ready = cart.items.length > 0 && blocker == null

  return (
    <aside className={`${storeCard} h-fit p-6 lg:sticky lg:top-24`}>
      <h2 className="text-lg font-semibold text-emerald-950">Order summary</h2>
      {showLines && cart.items.length > 0 ? (
        <ul className="mt-4 space-y-3">
          {cart.items.map((line) => (
            <li key={line.productId} className="flex items-start justify-between gap-3 text-sm">
              <span className="text-emerald-950">
                {line.name}
                <span className="text-slate-500"> × {line.qty}</span>
              </span>
              <span className="font-medium text-emerald-950">{formatInr(line.lineTotalPaise)}</span>
            </li>
          ))}
        </ul>
      ) : null}
      <dl className="mt-4 space-y-2 text-sm">
        <div className="flex items-center justify-between gap-3">
          <dt className="text-slate-600">Subtotal ({cart.itemCount})</dt>
          <dd className="text-base font-semibold text-emerald-950">{formatInr(cart.subtotalPaise)}</dd>
        </div>
      </dl>
      <p className="mt-3 text-xs leading-5 text-slate-500">
        Delivery is confirmed at payment. Prices stay live from the shop until you pay.
      </p>
      {blocker && cart.items.length > 0 ? <p className="mt-3 text-sm text-orange-800">{blocker}</p> : null}
      {cart.items.length === 0 ? (
        <p className="mt-3 text-sm text-slate-600">Add something from the shop before payment.</p>
      ) : null}
      {action}
      {showCheckout ? (
        ready ? (
          <Link to="/checkout" className={`${storeBtn} mt-5 inline-flex w-full justify-center`}>
            Proceed to checkout
          </Link>
        ) : (
          <button type="button" className={`${storeBtn} mt-5 w-full`} disabled>
            Proceed to checkout
          </button>
        )
      ) : null}
      <Link to="/shop" className={`${storeBtnGhost} mt-3 inline-flex w-full justify-center`}>
        Continue shopping
      </Link>
    </aside>
  )
}
