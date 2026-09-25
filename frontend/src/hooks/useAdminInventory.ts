import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminStock } from '../lib/adminDesk'

export const adminInventoryKey = ['admin', 'inventory'] as const

export type LowStockReport = {
  count: number
  items: { productId: string; available: number }[]
}

function idsQuery(ids: string[]): string {
  const params = new URLSearchParams()
  for (const id of ids) params.append('productIds', id)
  return params.toString()
}

export function useAdminInventory(productIds: string[]) {
  const key = [...productIds].sort().join(',')
  return useQuery({
    queryKey: [...adminInventoryKey, key],
    queryFn: () => api.get<AdminStock[]>(`/admin/inventory?${idsQuery(productIds)}`),
    enabled: productIds.length > 0
  })
}

export function useLowStock() {
  return useQuery({
    queryKey: ['admin', 'desk', 'low-stock'],
    queryFn: () => api.get<LowStockReport>('/admin/inventory/low-stock')
  })
}

export function useAdjustStock() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ productId, onHand }: { productId: string; onHand: number }) =>
      api.patch<AdminStock>(`/admin/inventory/${productId}`, { onHand }),
    onSuccess: (updated) => {
      queryClient.setQueriesData<AdminStock[]>({ queryKey: adminInventoryKey }, (current) => {
        if (!Array.isArray(current)) return current
        if (!current.some((stock) => stock.productId === updated.productId)) {
          return [...current, updated]
        }
        return current.map((stock) => (stock.productId === updated.productId ? updated : stock))
      })
      queryClient.invalidateQueries({ queryKey: ['admin', 'desk', 'low-stock'] })
    }
  })
}
