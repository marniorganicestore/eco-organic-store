import { storeBtnGhost } from './PageShell'

type PagerProps = {
  page: number
  size: number
  totalElements: number
  totalPages: number
  onPage: (page: number) => void
  label?: string
}

export function Pager({ page, size, totalElements, totalPages, onPage, label = 'Pagination' }: PagerProps) {
  if (totalPages <= 1) return null
  const start = totalElements === 0 ? 0 : page * size + 1
  const end = Math.min(totalElements, (page + 1) * size)
  const pages = windowPages(page, totalPages)

  return (
    <nav className="mt-6 flex flex-wrap items-center justify-between gap-3" aria-label={label}>
      <p className="text-sm text-slate-600">
        Showing {start}–{end} of {totalElements}
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <button type="button" className={storeBtnGhost} disabled={page <= 0} onClick={() => onPage(page - 1)}>
          Previous
        </button>
        {pages.map((entry, index) => entry === 'gap' ? (
          <span key={`gap-${index}`} className="px-1 text-slate-500" aria-hidden="true">…</span>
        ) : (
          <button
            key={entry}
            type="button"
            className={entry === page ? 'rounded-lg bg-emerald-700 px-3 py-1.5 text-sm font-medium text-white' : storeBtnGhost}
            aria-current={entry === page ? 'page' : undefined}
            onClick={() => onPage(entry)}
          >
            {entry + 1}
          </button>
        ))}
        <button type="button" className={storeBtnGhost} disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
          Next
        </button>
      </div>
    </nav>
  )
}

function windowPages(page: number, totalPages: number): Array<number | 'gap'> {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, index) => index)
  const pages = new Set<number>([0, totalPages - 1, page - 1, page, page + 1])
  const sorted = [...pages].filter((value) => value >= 0 && value < totalPages).sort((a, b) => a - b)
  const result: Array<number | 'gap'> = []
  for (const value of sorted) {
    const previous = result[result.length - 1]
    if (typeof previous === 'number' && value - previous > 1) result.push('gap')
    result.push(value)
  }
  return result
}
