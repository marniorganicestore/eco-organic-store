import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { harvestBtn, harvestCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { api } from '../../lib/api'

type OrderSummary = {
  id: string
  orderNumber: string
  orderStatus: string
}

export default function OrdersPage() {
  const orders = useQuery({
    queryKey: ['orders'],
    queryFn: () => api.get<OrderSummary[]>('/orders')
  })

  return (
    <PageShell title="My orders" subtitle="Track packed, shipped, and delivered harvests.">
      {orders.isPending ? <AccountSkeleton /> : null}
      {orders.isError ? <FormBanner tone="error">Unable to load orders. Refresh and try again.</FormBanner> : null}
      {orders.data && orders.data.length === 0 ? (
        <section className={`${harvestCard} p-8`}>
          <h2 className="font-semibold text-emerald-950">No orders yet</h2>
          <p className="mt-1 text-sm text-slate-600">When you check out, the harvest shows up here.</p>
          <Link to="/shop" className={`${harvestBtn} mt-4 inline-block`}>Browse the shop</Link>
        </section>
      ) : null}
      {orders.data && orders.data.length > 0 ? (
        <ul className={`${harvestCard} space-y-2 p-6`}>
          {orders.data.map((order) => (
            <li key={order.id} className="rounded-xl border border-emerald-100 bg-white/70 p-3 text-sm text-emerald-950">
              <span className="font-medium">{order.orderNumber}</span>
              <span className="text-slate-600"> · {order.orderStatus}</span>
            </li>
          ))}
        </ul>
      ) : null}
    </PageShell>
  )
}
