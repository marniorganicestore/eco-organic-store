import { useState } from 'react'
import { ApiError } from '../../lib/api'
import { isLowStock, onHandIssue, stockForProduct } from '../../lib/adminDesk'
import { useAdminProducts } from '../../hooks/useAdminCatalog'
import { useAdjustStock, useAdminInventory } from '../../hooks/useAdminInventory'
import { AdminPending } from '../../components/admin/AdminPending'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { PageShell, storeBtn, storeCard, storeInput } from '../../components/layout/PageShell'

export default function AdminInventoryPage() {
  const products = useAdminProducts()
  const inventory = useAdminInventory()
  const adjust = useAdjustStock()
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [error, setError] = useState('')
  const [savedId, setSavedId] = useState('')
  const pendingId = adjust.isPending ? adjust.variables?.productId : undefined

  async function save(productId: string, reserved: number) {
    const raw = drafts[productId]
    const message = onHandIssue(raw ?? '')
    if (message) {
      setError(message)
      return
    }
    setError('')
    setSavedId('')
    try {
      await adjust.mutateAsync({ productId, onHand: Number(raw) })
      setDrafts((current) => {
        const next = { ...current }
        delete next[productId]
        return next
      })
      setSavedId(productId)
      if (Number(raw) < reserved) {
        setError('')
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to update stock.')
    }
  }

  const rows = (products.data ?? []).map((product) => {
    const stock = stockForProduct(inventory.data ?? [], product.id)
    const typed = drafts[product.id]
    const onHandText = typed ?? String(stock.onHand)
    return { product, stock, onHandText, dirty: typed !== undefined && Number(typed) !== stock.onHand }
  })

  return (
    <PageShell
      title="Inventory"
      subtitle="On hand is what you hold. Reserved is held for unpaid checkout. Available is on hand minus reserved."
    >
      {products.isPending || inventory.isPending ? <AdminPending label="Loading inventory..." /> : null}
      {products.isError || inventory.isError ? <FormBanner tone="error">Unable to load inventory. Refresh and try again.</FormBanner> : null}
      {error ? <div className="mb-4"><FormBanner tone="error">{error}</FormBanner></div> : null}
      {products.data && inventory.data ? (
        rows.length === 0 ? (
          <p className={`${storeCard} p-6 text-sm text-slate-600`}>Add a product in the catalog before setting stock.</p>
        ) : (
          <div className={`${storeCard} overflow-x-auto`}>
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-emerald-800/80">
                <tr>
                  <th className="px-4 py-3 font-medium">Product</th>
                  <th className="px-4 py-3 font-medium">On hand</th>
                  <th className="px-4 py-3 font-medium">Reserved</th>
                  <th className="px-4 py-3 font-medium">Available</th>
                  <th className="px-4 py-3 font-medium">Update</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(({ product, stock, onHandText, dirty }) => {
                  const typedOnHand = /^\d+$/.test(onHandText) ? Number(onHandText) : stock.onHand
                  const nextAvailable = Math.max(0, typedOnHand - Math.min(stock.reserved, typedOnHand))
                  const low = isLowStock({ ...stock, available: dirty ? nextAvailable : stock.available })
                  return (
                    <tr key={product.id} className="border-t border-emerald-100">
                      <td className="px-4 py-3">
                        <p className="font-medium text-emerald-950">{product.name}</p>
                        <p className="text-slate-600">{product.unit}</p>
                      </td>
                      <td className="px-4 py-3">
                        <label className="sr-only" htmlFor={`on-hand-${product.id}`}>On hand for {product.name}</label>
                        <input
                          id={`on-hand-${product.id}`}
                          className={`${storeInput} w-24`}
                          inputMode="numeric"
                          value={onHandText}
                          onChange={(event) => {
                            setSavedId('')
                            setDrafts((current) => ({ ...current, [product.id]: event.target.value }))
                          }}
                        />
                      </td>
                      <td className="px-4 py-3">{stock.reserved}</td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-2">
                          <span>{dirty ? nextAvailable : stock.available}</span>
                          {low ? <StatusPill status="LOW" label="Low" /> : null}
                        </span>
                        {dirty && typedOnHand < stock.reserved ? (
                          <p className="mt-1 max-w-48 text-xs text-orange-800">Reserved holds above this number will be released.</p>
                        ) : null}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          className={storeBtn}
                          disabled={!dirty || pendingId === product.id}
                          onClick={() => save(product.id, stock.reserved)}
                        >
                          {pendingId === product.id ? 'Saving...' : savedId === product.id ? 'Saved' : 'Save'}
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )
      ) : null}
    </PageShell>
  )
}
