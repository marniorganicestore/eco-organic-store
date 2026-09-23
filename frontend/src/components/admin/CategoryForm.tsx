import { useState } from 'react'
import type { FormEvent } from 'react'
import { categoryIssue, slugify, type CategoryDraft } from '../../lib/adminDesk'
import { storeBtn, storeBtnGhost, storeCard } from '../layout/PageShell'
import { TextField } from '../account/TextField'
import { FormBanner } from '../account/FormBanner'

type CategoryFormProps = {
  initial: CategoryDraft
  title: string
  preserveSlug: boolean
  pending: boolean
  serverError: string
  onSubmit: (draft: CategoryDraft) => void
  onCancel: () => void
}

export function CategoryForm({
  initial,
  title,
  preserveSlug,
  pending,
  serverError,
  onSubmit,
  onCancel
}: CategoryFormProps) {
  const [draft, setDraft] = useState(initial)
  const [slugTouched, setSlugTouched] = useState(preserveSlug)
  const [error, setError] = useState('')

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = categoryIssue(draft)
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
          id="category-name"
          label="Name"
          value={draft.name}
          onChange={(name) => setDraft((current) => ({
            ...current,
            name,
            slug: slugTouched ? current.slug : slugify(name)
          }))}
        />
        <TextField
          id="category-slug"
          label="Slug"
          value={draft.slug}
          onChange={(slug) => {
            setSlugTouched(true)
            setDraft((current) => ({ ...current, slug }))
          }}
        />
        <TextField
          id="category-sort"
          label="Sort order"
          value={draft.sortOrder}
          inputMode="numeric"
          onChange={(sortOrder) => setDraft((current) => ({ ...current, sortOrder }))}
        />
        <TextField
          id="category-image"
          label="Image URL"
          value={draft.image}
          onChange={(image) => setDraft((current) => ({ ...current, image }))}
        />
      </div>
      <button type="submit" className={storeBtn} disabled={pending} aria-busy={pending}>
        {pending ? 'Saving...' : 'Save category'}
      </button>
    </form>
  )
}
