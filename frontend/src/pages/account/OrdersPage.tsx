import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { storeBtn, storeCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { OrderLineRow, type OrderLineView } from '../../components/order/OrderLineRow'
import { api } from '../../lib/api'
import { formatInr } from '../../lib/cart'
import { STORE_MAILBOX } from '../../lib/mail'

type OrderSummary = {
  id: string
  orderNumber: string
  lines: OrderLineView[]
  shippingAddress: string
  totalPaise: number
  orderStatus: string
  createdAt: string
}

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: 'Waiting for payment',
  CONFIRMED: 'Confirmed',
  PACKED: 'Packed',
  SHIPPED: 'On the way',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled'
}

function placedOn(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return 'Placed'
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  }).format(date)
}

export default function OrdersPage() {
  const orders = useQuery({
    queryKey: ['orders'],
    queryFn: () => api.get<OrderSummary[]>('/orders')
  })

  return (
    <PageShell title="My orders" subtitle="Track packed, shipped, and delivered orders.">
      {orders.isPending ? <AccountSkeleton /> : null}
      {orders.isError ? <FormBanner tone="error">Unable to load orders. Refresh and try again.</FormBanner> : null}
      {orders.data && orders.data.length === 0 ? (
        <section className={`${storeCard} p-8`}>
          <h2 className="font-semibold text-emerald-950">No orders yet</h2>
          <p className="mt-1 text-sm text-slate-600">When you check out, the order shows up here.</p>
          <Link to="/shop" className={`${storeBtn} mt-4 inline-block`}>Browse the shop</Link>
        </section>
      ) : null}
      {orders.data && orders.data.length > 0 ? (
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            Receipts and shipping notes come from {STORE_MAILBOX}.{' '}
            <Link className="font-medium text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/account/notifications">
              Email settings
            </Link>
          </p>
          <ul className="space-y-3">
            {orders.data.map((order) => (
              <li key={order.id} className={`${storeCard} p-4`}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium text-emerald-950">{order.orderNumber}</p>
                    <p className="text-sm text-slate-600">{placedOn(order.createdAt)} · {formatInr(order.totalPaise)}</p>
                    {order.shippingAddress ? <p className="mt-1 max-w-xl text-sm text-slate-600">{order.shippingAddress}</p> : null}
                  </div>
                  <p className="text-sm font-medium text-emerald-800">{STATUS_LABEL[order.orderStatus] ?? order.orderStatus}</p>
                </div>
                {(order.lines ?? []).length > 0 ? (
                  <ul className="mt-3 space-y-2">
                    {(order.lines ?? []).map((line) => (
                      <OrderLineRow key={`${order.id}-${line.productId}`} line={line} />
                    ))}
                  </ul>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </PageShell>
  )
}
