import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminReview } from '../lib/adminDesk'

export const adminReviewsKey = ['admin', 'reviews'] as const

export function useAdminReviews() {
  return useQuery({
    queryKey: adminReviewsKey,
    queryFn: () => api.get<AdminReview[]>('/admin/reviews')
  })
}

export function useModerateReview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ reviewId, status }: { reviewId: string; status: 'VISIBLE' | 'HIDDEN' }) =>
      api.patch<AdminReview>(`/admin/reviews/${reviewId}`, { status }),
    onSuccess: (updated) => {
      queryClient.setQueryData<AdminReview[]>(adminReviewsKey, (current) =>
        current?.map((review) => (review.id === updated.id ? updated : review)) ?? [updated]
      )
    }
  })
}
