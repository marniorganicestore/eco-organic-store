import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminStock } from '../lib/adminDesk'

export const adminInventoryKey = ['admin', 'inventory'] as const

export function useAdminInventory() {
  return useQuery({
    queryKey: adminInventoryKey,
    queryFn: () => api.get<AdminStock[]>('/admin/inventory')
  })
}

export function useAdjustStock() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, onHand }: { productId: string; onHand: number }) =>
      api.patch<AdminStock>(`/admin/inventory/${productId}`, { onHand }),
    onSuccess: (updated) => {
      queryClient.setQueryData<AdminStock[]>(adminInventoryKey, (current) => {
        if (!current?.some((stock) => stock.productId === updated.productId)) {
          return [...(current ?? []), updated]
        }
        return current.map((stock) => (stock.productId === updated.productId ? updated : stock))
      })
    }
  })
}
