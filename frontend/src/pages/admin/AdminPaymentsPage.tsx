import { formatInr, formatWhen } from '../../lib/adminDesk'
import { useAdminPayments } from '../../hooks/useAdminPayments'
import { AdminPending } from '../../components/admin/AdminPending'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { PageShell, storeCard } from '../../components/layout/PageShell'

export default function AdminPaymentsPage() {
  const payments = useAdminPayments()

  return (
    <PageShell title="Payments" subtitle="Stripe checkout records. A paid payment confirms the order and the stock hold.">
      {payments.isPending ? <AdminPending label="Loading payments..." /> : null}
      {payments.isError ? <FormBanner tone="error">Unable to load payments. Refresh and try again.</FormBanner> : null}
      {payments.data && payments.data.length === 0 ? (
        <p className={`${storeCard} p-6 text-sm text-slate-600`}>No payments yet. They appear when a customer starts checkout.</p>
      ) : null}
      {payments.data && payments.data.length > 0 ? (
        <div className={`${storeCard} overflow-x-auto`}>
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="text-xs uppercase tracking-wide text-emerald-800/80">
              <tr>
                <th className="px-4 py-3 font-medium">Order</th>
                <th className="px-4 py-3 font-medium">Amount</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Opened</th>
              </tr>
            </thead>
            <tbody>
              {payments.data.map((payment) => (
                <tr key={payment.id} className="border-t border-emerald-100">
                  <td className="px-4 py-3 font-medium text-emerald-950">{payment.orderNumber}</td>
                  <td className="px-4 py-3">{formatInr(payment.amountPaise)}</td>
                  <td className="px-4 py-3"><StatusPill status={payment.status} /></td>
                  <td className="px-4 py-3 text-slate-600">{formatWhen(payment.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </PageShell>
  )
}
