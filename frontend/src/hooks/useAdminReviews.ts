import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminReview } from '../lib/adminDesk'
import { ADMIN_PAGE_SIZE, pageQuery, type PageResult } from '../lib/page'

export const adminReviewsKey = ['admin', 'reviews'] as const

export function useAdminReviews(page: number, status?: 'HIDDEN' | 'VISIBLE') {
  return useQuery({
    queryKey: [...adminReviewsKey, status ?? 'ALL', page],
    queryFn: () => api.get<PageResult<AdminReview>>(`/admin/reviews${pageQuery(page, ADMIN_PAGE_SIZE, { status })}`)
  })
}

export function useReviewDesk() {
  return useQuery({
    queryKey: ['admin', 'desk', 'reviews'],
    queryFn: () => api.get<{ hiddenCount: number }>('/admin/reviews/summary')
  })
}

export function useModerateReview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ reviewId, status }: { reviewId: string; status: 'VISIBLE' | 'HIDDEN' }) =>
      api.patch<AdminReview>(`/admin/reviews/${reviewId}`, { status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminReviewsKey })
      queryClient.invalidateQueries({ queryKey: ['admin', 'desk', 'reviews'] })
    }
  })
}
