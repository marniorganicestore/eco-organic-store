import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../lib/api'
import { STORE_MAILBOX } from '../../lib/mail'
import { useNotificationPreferences, useUpdateNotificationPreferences } from '../../hooks/useNotificationPreferences'
import { storeBtn, storeCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'

export default function NotificationsPage() {
  const preferences = useNotificationPreferences()
  const update = useUpdateNotificationPreferences()
  const [orderUpdates, setOrderUpdates] = useState(true)
  const [seeded, setSeeded] = useState(false)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const loaded = preferences.data
  if (loaded && !seeded) {
    setSeeded(true)
    setOrderUpdates(loaded.orderUpdates)
  }
  const fromAddress = loaded?.fromAddress || STORE_MAILBOX

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaved(false)
    setError('')
    try {
      const next = await update.mutateAsync(orderUpdates)
      setOrderUpdates(next.orderUpdates)
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to save email preferences. Please try again.')
    }
  }

  return (
    <PageShell title="Email" subtitle="Receipts and delivery notes, sent from the store mailbox.">
      {preferences.isPending ? <AccountSkeleton /> : null}
      {preferences.isError ? <FormBanner tone="error">Unable to load email preferences. Refresh and try again.</FormBanner> : null}
      {loaded ? (
        <form className={`${storeCard} max-w-lg space-y-5 p-6`} onSubmit={submit}>
          {error ? <FormBanner tone="error">{error}</FormBanner> : null}
          {saved ? <FormBanner tone="success">Email preferences saved.</FormBanner> : null}
          <p className="text-sm text-slate-600">
            Messages for <span className="font-medium text-emerald-950">{loaded.email}</span> come from{' '}
            <span className="font-medium text-emerald-950">{fromAddress}</span>.
          </p>
          <label className="flex items-start gap-3 rounded-xl border border-emerald-100 bg-white/70 p-4">
            <input
              id="order-updates"
              className="mt-1 h-4 w-4 accent-emerald-700"
              type="checkbox"
              checked={orderUpdates}
              disabled={update.isPending}
              onChange={(event) => setOrderUpdates(event.target.checked)}
            />
            <span>
              <span className="block font-medium text-emerald-950">Order updates</span>
              <span className="mt-1 block text-sm text-slate-600">
                A receipt when payment clears, then a note when the order is packed, shipped, and delivered.
              </span>
            </span>
          </label>
          <p className="text-sm text-slate-600">
            Password resets and password changes are always emailed. The store desk still receives a copy of each order.
          </p>
          <button className={storeBtn} type="submit" disabled={update.isPending} aria-busy={update.isPending}>
            {update.isPending ? 'Saving...' : 'Save email preferences'}
          </button>
        </form>
      ) : null}
    </PageShell>
  )
}
