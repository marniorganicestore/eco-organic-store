import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { storeBtn, storeCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { api } from '../../lib/api'
import { STORE_MAILBOX } from '../../lib/mail'

type OrderSummary = {
  id: string
  orderNumber: string
  orderStatus: string
}

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: 'Waiting for payment',
  CONFIRMED: 'Confirmed',
  PACKED: 'Packed',
  SHIPPED: 'On the way',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled'
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
          <ul className={`${storeCard} space-y-2 p-6`}>
            {orders.data.map((order) => (
              <li key={order.id} className="rounded-xl border border-emerald-100 bg-white/70 p-3 text-sm text-emerald-950">
                <span className="font-medium">{order.orderNumber}</span>
                <span className="text-slate-600"> · {STATUS_LABEL[order.orderStatus] ?? order.orderStatus}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </PageShell>
  )
}
