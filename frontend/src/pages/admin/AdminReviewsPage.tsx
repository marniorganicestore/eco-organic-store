import { useMemo, useState } from 'react'
import { ApiError } from '../../lib/api'
import { formatWhen } from '../../lib/adminDesk'
import { useAdminProductLookup } from '../../hooks/useAdminCatalog'
import { useAdminReviews, useModerateReview } from '../../hooks/useAdminReviews'
import { Pager } from '../../components/layout/Pager'
import { useClampPage } from '../../hooks/useClampPage'
import { AdminPending } from '../../components/admin/AdminPending'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { PageShell, storeBtn, storeBtnGhost, storeCard } from '../../components/layout/PageShell'

type Filter = 'HIDDEN' | 'VISIBLE' | 'ALL'

export default function AdminReviewsPage() {
  const [filter, setFilter] = useState<Filter>('HIDDEN')
  const [page, setPage] = useState(0)
  const reviews = useAdminReviews(page, filter === 'ALL' ? undefined : filter)
  const productIds = useMemo(() => [...new Set((reviews.data?.items ?? []).map((review) => review.productId))], [reviews.data])
  const products = useAdminProductLookup(productIds)
  const moderate = useModerateReview()
  const [error, setError] = useState('')
  const pendingId = moderate.isPending ? moderate.variables?.reviewId : undefined
  const visible = reviews.data?.items ?? []
  useClampPage(page, reviews.data?.totalPages, setPage)

  const names = useMemo(() => {
    const map = new Map<string, string>()
    for (const product of products.data ?? []) map.set(product.id, product.name)
    return map
  }, [products.data])

  async function setStatus(reviewId: string, status: 'VISIBLE' | 'HIDDEN') {
    setError('')
    try {
      await moderate.mutateAsync({ reviewId, status })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to update that review.')
    }
  }

  return (
    <PageShell
      title="Reviews"
      subtitle="Hidden reviews stay off the product page. Making one visible updates that product's rating."
      actions={(
        <div className="inline-flex rounded-lg border border-emerald-200 bg-white/80 p-0.5" role="group" aria-label="Review filter">
          {(['HIDDEN', 'VISIBLE', 'ALL'] as const).map((value) => (
            <button
              key={value}
              type="button"
              aria-pressed={filter === value}
              className={`rounded-md px-3 py-1.5 text-sm font-medium outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 ${
                filter === value ? 'bg-emerald-700 text-white' : 'text-emerald-900 hover:bg-emerald-50'
              }`}
              onClick={() => { setFilter(value); setPage(0) }}
            >
              {value === 'ALL' ? 'All' : value === 'HIDDEN' ? 'Hidden' : 'Visible'}
            </button>
          ))}
        </div>
      )}
    >
      {reviews.isPending ? <AdminPending label="Loading reviews..." /> : null}
      {reviews.isError ? <FormBanner tone="error">Unable to load reviews. Refresh and try again.</FormBanner> : null}
      {error ? <div className="mb-4"><FormBanner tone="error">{error}</FormBanner></div> : null}
      {reviews.data && visible.length === 0 ? (
        <p className={`${storeCard} p-6 text-sm text-slate-600`}>
          {filter === 'HIDDEN' ? 'No hidden reviews. The shop is showing every verified review.' : 'No reviews in this view.'}
        </p>
      ) : null}
      {visible.length > 0 ? (
        <ul className="space-y-3">
          {visible.map((review) => {
            const hidden = review.status === 'HIDDEN'
            return (
              <li key={review.id} className={`${storeCard} p-4`}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium text-emerald-950">{names.get(review.productId) ?? 'Product'}</p>
                    <p className="text-sm text-slate-600">{review.rating}★ · {formatWhen(review.createdAt)}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusPill status={review.status} />
                    <button
                      type="button"
                      className={hidden ? storeBtn : storeBtnGhost}
                      disabled={pendingId === review.id}
                      onClick={() => setStatus(review.id, hidden ? 'VISIBLE' : 'HIDDEN')}
                    >
                      {pendingId === review.id ? 'Saving...' : hidden ? 'Make visible' : 'Hide'}
                    </button>
                  </div>
                </div>
                <p className="mt-3 text-sm text-slate-700">{review.body}</p>
              </li>
            )
          })}
        </ul>
      ) : null}
      {reviews.data ? (
        <Pager
          page={reviews.data.page}
          size={reviews.data.size}
          totalElements={reviews.data.totalElements}
          totalPages={reviews.data.totalPages}
          onPage={setPage}
          label="Review pages"
        />
      ) : null}
    </PageShell>
  )
}
