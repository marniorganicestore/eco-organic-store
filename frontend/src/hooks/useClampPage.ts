import { useEffect } from 'react'

export function useClampPage(page: number, totalPages: number | undefined, onPage: (page: number) => void) {
  useEffect(() => {
    if (totalPages === undefined) return
    const next = totalPages <= 0 ? 0 : Math.min(page, totalPages - 1)
    if (next !== page) onPage(next)
  }, [page, totalPages, onPage])
}
