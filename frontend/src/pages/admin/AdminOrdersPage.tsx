import { useState } from 'react'
import { ApiError } from '../../lib/api'
import { formatInr, formatWhen, nextFulfillment } from '../../lib/adminDesk'
import { useAdvanceOrder, useAdminOrders } from '../../hooks/useAdminOrders'
import { AdminPending } from '../../components/admin/AdminPending'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { PageShell, storeBtn, storeCard } from '../../components/layout/PageShell'

export default function AdminOrdersPage() {
  const orders = useAdminOrders()
  const advance = useAdvanceOrder()
  const [error, setError] = useState('')
  const pendingNumber = advance.isPending ? advance.variables?.orderNumber : undefined

  async function move(orderNumber: string, status: string) {
    setError('')
    try {
      await advance.mutateAsync({ orderNumber, status })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to update that order.')
    }
  }

  return (
    <PageShell
      title="Orders"
      subtitle="Paid orders move one step at a time: packed, then shipped, then delivered. Each step emails the customer when order updates are on."
    >
      {orders.isPending ? <AdminPending label="Loading orders..." /> : null}
      {orders.isError ? <FormBanner tone="error">Unable to load orders. Refresh and try again.</FormBanner> : null}
      {error ? <div className="mb-4"><FormBanner tone="error">{error}</FormBanner></div> : null}
      {orders.data && orders.data.length === 0 ? (
        <p className={`${storeCard} p-6 text-sm text-slate-600`}>No orders yet. They appear here after checkout.</p>
      ) : null}
      {orders.data && orders.data.length > 0 ? (
        <ul className="space-y-3">
          {orders.data.map((order) => {
            const next = nextFulfillment(order.orderStatus)
            return (
              <li key={order.id} className={`${storeCard} p-4`}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium text-emerald-950">{order.orderNumber}</p>
                    <p className="text-sm text-slate-600">{formatWhen(order.createdAt)} · {formatInr(order.totalPaise)}</p>
                    <p className="mt-1 max-w-xl text-sm text-slate-600">{order.shippingAddress}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusPill status={order.orderStatus} />
                    {next ? (
                      <button
                        type="button"
                        className={storeBtn}
                        disabled={pendingNumber === order.orderNumber}
                        onClick={() => move(order.orderNumber, next.status)}
                      >
                        {pendingNumber === order.orderNumber ? 'Saving...' : next.label}
                      </button>
                    ) : null}
                  </div>
                </div>
                <ul className="mt-3 space-y-1 text-sm text-emerald-950">
                  {order.lines.map((line) => (
                    <li key={`${order.id}-${line.productId}`}>
                      {line.productName} × {line.qty}
                      <span className="text-slate-600"> · {formatInr(line.pricePaise)}</span>
                    </li>
                  ))}
                </ul>
              </li>
            )
          })}
        </ul>
      ) : null}
    </PageShell>
  )
}
