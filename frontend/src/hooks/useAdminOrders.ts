import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminOrder } from '../lib/adminDesk'

export const adminOrdersKey = ['admin', 'orders'] as const

export function useAdminOrders() {
  return useQuery({
    queryKey: adminOrdersKey,
    queryFn: () => api.get<AdminOrder[]>('/admin/orders')
  })
}

export function useAdvanceOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderNumber, status }: { orderNumber: string; status: string }) =>
      api.patch<AdminOrder>(`/admin/orders/${orderNumber}`, { status }),
    onSuccess: (updated) => {
      queryClient.setQueryData<AdminOrder[]>(adminOrdersKey, (current) =>
        current?.map((order) => (order.orderNumber === updated.orderNumber ? updated : order)) ?? [updated]
      )
    }
  })
}
