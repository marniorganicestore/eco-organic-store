import { Link } from 'react-router-dom'
import { AdminPending } from '../../components/admin/AdminPending'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { storeCard, PageShell } from '../../components/layout/PageShell'
import { useAdminProductLookup, useCatalogDesk } from '../../hooks/useAdminCatalog'
import { useLowStock } from '../../hooks/useAdminInventory'
import { useOrderDesk } from '../../hooks/useAdminOrders'
import { usePaymentDesk } from '../../hooks/useAdminPayments'
import { useReviewDesk } from '../../hooks/useAdminReviews'
import { formatInr, formatWhen } from '../../lib/adminDesk'

export default function AdminDashboardPage() {
  const catalog = useCatalogDesk()
  const orders = useOrderDesk()
  const payments = usePaymentDesk()
  const reviews = useReviewDesk()
  const lowStock = useLowStock()
  const names = useAdminProductLookup((lowStock.data?.items ?? []).map((item) => item.productId))
  const loading = catalog.isPending || orders.isPending || reviews.isPending || lowStock.isPending
  const productName = new Map((names.data ?? []).map((product) => [product.id, product.name]))
  const pendingPayments = payments.data?.pendingCount ?? 0

  return (
    <PageShell title="Overview" subtitle="Today's orders, stock that needs a refill, and reviews waiting for a look.">
      {loading ? <AdminPending label="Loading the store desk..." /> : null}
      {catalog.isError || lowStock.isError || orders.isError || reviews.isError ? (
        <div className="mb-4">
          <FormBanner tone="error">Some desk numbers could not be loaded. Open the section and try again.</FormBanner>
        </div>
      ) : null}
      {!loading ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Metric to="/admin/orders" label="Today's orders" value={orders.isError ? '—' : String(orders.data?.todayCount ?? 0)} hint={formatInr(orders.data?.todayTotalPaise ?? 0)} />
            <Metric to="/admin/orders" label="Ready to pack" value={orders.isError ? '—' : String(orders.data?.confirmedCount ?? 0)} hint="Paid and waiting" />
            <Metric to="/admin/inventory" label="Low stock" value={lowStock.isError ? '—' : String(lowStock.data?.count ?? 0)} hint="10 or fewer available" />
            <Metric to="/admin/reviews" label="Hidden reviews" value={reviews.isError ? '—' : String(reviews.data?.hiddenCount ?? 0)} hint="Off the product page" />
          </div>
          <p className="mt-4 text-sm text-slate-600">
            {catalog.data ? `${catalog.data.categoryCount} categories · ${catalog.data.productCount} products. ` : null}
            <Link className="font-medium text-emerald-800 underline-offset-2 hover:underline" to="/admin/payments">
              {payments.isError ? 'Payments need a refresh.' : `${pendingPayments} payment${pendingPayments === 1 ? '' : 's'} still pending.`}
            </Link>
          </p>
          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <section className={`${storeCard} p-5`}>
              <h2 className="font-semibold text-emerald-950">Ready to pack</h2>
              {(orders.data?.confirmed.length ?? 0) === 0 ? <p className="mt-3 text-sm text-slate-600">Nothing is waiting to be packed.</p> : (
                <ul className="mt-3 space-y-2">
                  {orders.data?.confirmed.map((order) => (
                    <li key={order.id} className="flex items-center justify-between gap-3 rounded-xl border border-emerald-100 bg-white/75 px-3 py-2 text-sm">
                      <span>
                        <span className="font-medium text-emerald-950">{order.orderNumber}</span>
                        <span className="text-slate-600"> · {formatInr(order.totalPaise)}</span>
                      </span>
                      <StatusPill status={order.orderStatus} />
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section className={`${storeCard} p-5`}>
              <h2 className="font-semibold text-emerald-950">Low stock</h2>
              {(lowStock.data?.items.length ?? 0) === 0 ? <p className="mt-3 text-sm text-slate-600">Active products have enough on hand.</p> : (
                <ul className="mt-3 space-y-2">
                  {lowStock.data?.items.map((item) => (
                    <li key={item.productId} className="flex items-center justify-between gap-3 rounded-xl border border-emerald-100 bg-white/75 px-3 py-2 text-sm">
                      <span className="font-medium text-emerald-950">{productName.get(item.productId) ?? 'Product'}</span>
                      <StatusPill status="LOW" label={`${item.available} available`} />
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section className={`${storeCard} p-5 lg:col-span-2`}>
              <h2 className="font-semibold text-emerald-950">Orders placed today</h2>
              {(orders.data?.today.length ?? 0) === 0 ? <p className="mt-3 text-sm text-slate-600">No orders have come in today.</p> : (
                <ul className="mt-3 space-y-2">
                  {orders.data?.today.map((order) => (
                    <li key={order.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-emerald-100 bg-white/75 px-3 py-2 text-sm">
                      <span className="font-medium text-emerald-950">{order.orderNumber}</span>
                      <span className="text-slate-600">{formatWhen(order.createdAt)}</span>
                      <span>{formatInr(order.totalPaise)}</span>
                      <StatusPill status={order.orderStatus} />
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      ) : null}
    </PageShell>
  )
}

function Metric({ to, label, value, hint }: { to: string; label: string; value: string; hint: string }) {
  return (
    <Link to={to} className={`${storeCard} block p-4 outline-none focus-visible:ring-2 focus-visible:ring-emerald-700`}>
      <p className="text-xs font-medium uppercase tracking-[0.16em] text-emerald-800/80">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-emerald-950">{value}</p>
      <p className="mt-1 text-sm text-slate-600">{hint}</p>
    </Link>
  )
}
