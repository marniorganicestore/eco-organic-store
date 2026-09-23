import { useState } from 'react'
import type { FormEvent } from 'react'
import { productIssue, slugify, type AdminCategory, type ProductDraft } from '../../lib/adminDesk'
import { storeBtn, storeBtnGhost, storeCard, storeInput } from '../layout/PageShell'
import { TextField } from '../account/TextField'
import { FormBanner } from '../account/FormBanner'

type ProductFormProps = {
  categories: AdminCategory[]
  initial: ProductDraft
  title: string
  preserveSlug: boolean
  pending: boolean
  serverError: string
  onSubmit: (draft: ProductDraft) => void
  onCancel: () => void
}

export function ProductForm({
  categories,
  initial,
  title,
  preserveSlug,
  pending,
  serverError,
  onSubmit,
  onCancel
}: ProductFormProps) {
  const [draft, setDraft] = useState(initial)
  const [slugTouched, setSlugTouched] = useState(preserveSlug)
  const [error, setError] = useState('')

  function update(patch: Partial<ProductDraft>) {
    setDraft((current) => ({ ...current, ...patch }))
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = productIssue(draft)
    if (message) {
      setError(message)
      return
    }
    setError('')
    onSubmit(draft)
  }

  return (
    <form className={`${storeCard} space-y-4 p-5`} onSubmit={submit}>
      <div className="flex items-start justify-between gap-3">
        <h2 className="text-lg font-semibold text-emerald-950">{title}</h2>
        <button type="button" className={storeBtnGhost} onClick={onCancel}>Close</button>
      </div>
      {error || serverError ? <FormBanner tone="error">{error || serverError}</FormBanner> : null}
      <div className="grid gap-4 sm:grid-cols-2">
        <TextField
          id="product-name"
          label="Name"
          value={draft.name}
          onChange={(name) => {
            update({ name, slug: slugTouched ? draft.slug : slugify(name) })
          }}
        />
        <TextField
          id="product-slug"
          label="Slug"
          value={draft.slug}
          hint="Used in the shop URL. Leave it to follow the name."
          onChange={(slug) => {
            setSlugTouched(true)
            update({ slug })
          }}
        />
        <TextField
          id="product-price"
          label="Price (₹)"
          value={draft.priceRupees}
          inputMode="decimal"
          hint="Stored in paise. 179.00 becomes ₹179.00."
          onChange={(priceRupees) => update({ priceRupees })}
        />
        <TextField id="product-unit" label="Unit" value={draft.unit} hint="Example: 500g" onChange={(unit) => update({ unit })} />
        <label className="block text-sm font-medium text-slate-800" htmlFor="product-category">
          Category
          <select
            id="product-category"
            className={`${storeInput} mt-1`}
            value={draft.categoryId}
            onChange={(event) => update({ categoryId: event.target.value })}
          >
            <option value="">Choose a category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
        </label>
        <TextField id="product-origin" label="Origin" value={draft.origin} onChange={(origin) => update({ origin })} />
      </div>
      <label className="block text-sm font-medium text-slate-800" htmlFor="product-description">
        Description
        <textarea
          id="product-description"
          className={`${storeInput} mt-1 min-h-24`}
          value={draft.description}
          onChange={(event) => update({ description: event.target.value })}
        />
      </label>
      <TextField
        id="product-image"
        label="Image URL"
        value={draft.image}
        hint="A direct image address. Uploads stay out of this desk."
        onChange={(image) => update({ image })}
      />
      <TextField
        id="product-certifications"
        label="Certifications"
        value={draft.certifications}
        hint="Separate names with commas."
        onChange={(certifications) => update({ certifications })}
      />
      <div className="flex flex-wrap gap-4">
        <label className="flex items-center gap-2 text-sm text-emerald-950">
          <input
            type="checkbox"
            className="size-4 accent-emerald-700"
            checked={draft.featured}
            onChange={(event) => update({ featured: event.target.checked })}
          />
          Featured
        </label>
        <label className="flex items-center gap-2 text-sm text-emerald-950">
          <input
            type="checkbox"
            className="size-4 accent-emerald-700"
            checked={draft.active}
            onChange={(event) => update({ active: event.target.checked })}
          />
          Visible in the shop
        </label>
      </div>
      <button type="submit" className={storeBtn} disabled={pending} aria-busy={pending}>
        {pending ? 'Saving...' : 'Save product'}
      </button>
    </form>
  )
}
