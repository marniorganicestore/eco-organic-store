export type PageResult<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export const SHOP_PAGE_SIZE = 12
export const REVIEW_PAGE_SIZE = 8
export const ORDER_PAGE_SIZE = 10
export const ADMIN_PAGE_SIZE = 20

export function pageQuery(page: number, size: number, extra?: Record<string, string | undefined>): string {
  const params = new URLSearchParams()
  params.set('page', String(Math.max(0, page)))
  params.set('size', String(size))
  if (extra) {
    for (const [key, value] of Object.entries(extra)) {
      if (value) params.set(key, value)
    }
  }
  return `?${params.toString()}`
}

export function idsQuery(ids: string[]): string {
  const params = new URLSearchParams()
  for (const id of ids) {
    if (id) params.append('ids', id)
  }
  return params.toString()
}
