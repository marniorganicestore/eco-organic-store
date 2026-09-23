import { Link } from 'react-router-dom'
import { AdminPending } from '../../components/admin/AdminPending'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { storeCard, PageShell } from '../../components/layout/PageShell'
import { useAdminCategories, useAdminProducts } from '../../hooks/useAdminCatalog'
import { useAdminInventory } from '../../hooks/useAdminInventory'
import { useAdminOrders } from '../../hooks/useAdminOrders'
import { useAdminPayments } from '../../hooks/useAdminPayments'
import { useAdminReviews } from '../../hooks/useAdminReviews'
import { formatInr, formatWhen, isLowStock, isSameLocalDay, stockForProduct } from '../../lib/adminDesk'

export default function AdminDashboardPage() {
  const products = useAdminProducts()
  const categories = useAdminCategories()
  const inventory = useAdminInventory()
  const orders = useAdminOrders()
  const payments = useAdminPayments()
  const reviews = useAdminReviews()
  const loading = products.isPending || inventory.isPending || orders.isPending || reviews.isPending

  const productRows = products.data ?? []
  const stockRows = inventory.data ?? []
  const orderRows = orders.data ?? []
  const reviewRows = reviews.data ?? []
  const today = orderRows.filter((order) => isSameLocalDay(order.createdAt))
  const toPack = orderRows.filter((order) => order.orderStatus === 'CONFIRMED')
  const low = productRows.filter((product) => product.active && isLowStock(stockForProduct(stockRows, product.id)))
  const hidden = reviewRows.filter((review) => review.status === 'HIDDEN')
  const pendingPayments = (payments.data ?? []).filter((payment) => payment.status === 'PENDING')

  return (
    <PageShell title="Overview" subtitle="Today's orders, stock that needs a refill, and reviews waiting for a look.">
      {loading ? <AdminPending label="Loading the store desk..." /> : null}
      {products.isError || inventory.isError || orders.isError || reviews.isError ? (
        <div className="mb-4">
          <FormBanner tone="error">Some desk numbers could not be loaded. Open the section and try again.</FormBanner>
        </div>
      ) : null}
      {!loading ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <Metric to="/admin/orders" label="Today's orders" value={orders.isError ? '—' : String(today.length)} hint={formatInr(today.reduce((sum, order) => sum + order.totalPaise, 0))} />
            <Metric to="/admin/orders" label="Ready to pack" value={orders.isError ? '—' : String(toPack.length)} hint="Paid and waiting" />
            <Metric to="/admin/inventory" label="Low stock" value={products.isError || inventory.isError ? '—' : String(low.length)} hint="10 or fewer available" />
            <Metric to="/admin/reviews" label="Hidden reviews" value={reviews.isError ? '—' : String(hidden.length)} hint="Off the product page" />
          </div>
          <p className="mt-4 text-sm text-slate-600">
            {categories.data ? `${categories.data.length} categories · ${productRows.length} products. ` : null}
            <Link className="font-medium text-emerald-800 underline-offset-2 hover:underline" to="/admin/payments">
              {payments.isError ? 'Payments need a refresh.' : `${pendingPayments.length} payment${pendingPayments.length === 1 ? '' : 's'} still pending.`}
            </Link>
          </p>
          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <section className={`${storeCard} p-5`}>
              <h2 className="font-semibold text-emerald-950">Ready to pack</h2>
              {toPack.length === 0 ? <p className="mt-3 text-sm text-slate-600">Nothing is waiting to be packed.</p> : (
                <ul className="mt-3 space-y-2">
                  {toPack.slice(0, 5).map((order) => (
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
              {low.length === 0 ? <p className="mt-3 text-sm text-slate-600">Active products have enough on hand.</p> : (
                <ul className="mt-3 space-y-2">
                  {low.slice(0, 5).map((product) => {
                    const stock = stockForProduct(stockRows, product.id)
                    return (
                      <li key={product.id} className="flex items-center justify-between gap-3 rounded-xl border border-emerald-100 bg-white/75 px-3 py-2 text-sm">
                        <span className="font-medium text-emerald-950">{product.name}</span>
                        <StatusPill status="LOW" label={`${stock.available} available`} />
                      </li>
                    )
                  })}
                </ul>
              )}
            </section>
            <section className={`${storeCard} p-5 lg:col-span-2`}>
              <h2 className="font-semibold text-emerald-950">Orders placed today</h2>
              {today.length === 0 ? <p className="mt-3 text-sm text-slate-600">No orders have come in today.</p> : (
                <ul className="mt-3 space-y-2">
                  {today.slice(0, 6).map((order) => (
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
