import { Link } from 'react-router-dom'
import { useCart, useSetCartQty } from '../hooks/useCart'
import { ApiError } from '../lib/api'
import { formatInr, lineNote, MAX_CART_QTY } from '../lib/cart'
import { resolveImageSrc } from '../lib/media'
import { FormBanner } from '../components/account/FormBanner'
import { storeBtn, storeCard, PageShell } from '../components/layout/PageShell'
import { CartSummary } from '../components/cart/CartSummary'
import { QtyStepper } from '../components/cart/QtyStepper'
import type { CartLine } from '../store/cartStore'

export default function CartPage() {
  const cart = useCart()
  const setQty = useSetCartQty()
  const pendingId = setQty.isPending ? setQty.variables?.productId : undefined

  function changeQty(productId: string, qty: number) {
    setQty.mutate({ productId, qty })
  }

  return (
    <PageShell title="Cart" subtitle="Review your basket before checkout.">
      {cart.isPending ? <CartSkeleton /> : null}
      {cart.isError ? (
        <div className={`${storeCard} space-y-4 p-6`}>
          <FormBanner tone="error">
            {cart.error instanceof ApiError ? cart.error.message : 'We could not load your basket. Try again.'}
          </FormBanner>
          <button type="button" className={storeBtn} onClick={() => cart.refetch()}>Try again</button>
        </div>
      ) : null}
      {cart.data && cart.data.items.length === 0 ? <EmptyBasket /> : null}
      {cart.data && cart.data.items.length > 0 ? (
        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
          <ul className={`${storeCard} divide-y divide-emerald-100`}>
            {cart.data.items.map((line) => (
              <CartLineRow
                key={line.productId}
                line={line}
                pending={pendingId === line.productId}
                onChange={(qty) => changeQty(line.productId, qty)}
                onRemove={() => changeQty(line.productId, 0)}
              />
            ))}
          </ul>
          <CartSummary cart={cart.data} />
        </div>
      ) : null}
    </PageShell>
  )
}

function CartLineRow({
  line,
  pending,
  onChange,
  onRemove
}: {
  line: CartLine
  pending: boolean
  onChange: (qty: number) => void
  onRemove: () => void
}) {
  const note = lineNote(line)
  const image = resolveImageSrc(line.image)
  const canAdjust = Boolean(line.slug) && (line.available == null || line.available > 0)
  const max = line.available == null ? MAX_CART_QTY : Math.min(MAX_CART_QTY, line.available)
  const noteClass = note && note.endsWith('left.') ? 'text-emerald-800' : 'text-orange-800'

  return (
    <li className="flex gap-4 p-4 sm:p-5">
      {image && line.slug ? (
        <Link to={`/product/${line.slug}`} className="shrink-0 rounded-xl outline-none focus-visible:ring-2 focus-visible:ring-emerald-700" aria-label={`Photo of ${line.name}`}>
          <img src={image} alt="" className="h-24 w-24 rounded-xl object-cover" />
        </Link>
      ) : (
        <span className="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-xs text-emerald-800">Organic</span>
      )}
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            {line.slug ? (
              <Link to={`/product/${line.slug}`} className="font-medium text-emerald-950 hover:underline">
                {line.name}
              </Link>
            ) : (
              <p className="font-medium text-emerald-950">{line.name}</p>
            )}
            {line.unit || line.origin ? (
              <p className="mt-0.5 text-sm text-slate-500">{[line.unit, line.origin].filter(Boolean).join(' · ')}</p>
            ) : null}
          </div>
          <p className="font-semibold text-emerald-950">{formatInr(line.lineTotalPaise)}</p>
        </div>
        {line.pricePaise > 0 ? <p className="mt-1 text-sm text-slate-600">{formatInr(line.pricePaise)} each</p> : null}
        {note ? <p className={`mt-1 text-sm ${noteClass}`}>{note}</p> : null}
        <div className="mt-3 flex flex-wrap items-center gap-3">
          {canAdjust ? (
            <QtyStepper qty={line.qty} max={max} label={line.name} disabled={pending} onChange={onChange} />
          ) : null}
          <button
            type="button"
            className="text-sm font-medium text-orange-800 underline-offset-2 outline-none hover:underline focus-visible:ring-2 focus-visible:ring-emerald-700 disabled:opacity-40"
            onClick={onRemove}
            disabled={pending}
            aria-label={`Remove ${line.name}`}
          >
            Remove
          </button>
        </div>
      </div>
    </li>
  )
}

function EmptyBasket() {
  return (
    <div className={`${storeCard} px-6 py-12 text-center`}>
      <h2 className="text-xl font-semibold text-emerald-950">Your basket is empty</h2>
      <p className="mx-auto mt-2 max-w-md text-sm text-slate-600">
        Seasonal produce and pantry staples are waiting in the shop.
      </p>
      <Link to="/shop" className={`${storeBtn} mt-6 inline-flex`}>Shop organic produce</Link>
    </div>
  )
}

function CartSkeleton() {
  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]" aria-busy="true" aria-label="Loading cart">
      <div className={`${storeCard} space-y-4 p-4`}>
        <div className="h-24 animate-pulse rounded-xl bg-emerald-100/80" />
        <div className="h-24 animate-pulse rounded-xl bg-emerald-100/80" />
      </div>
      <div className={`${storeCard} h-48 animate-pulse bg-emerald-100/60`} />
    </div>
  )
}
