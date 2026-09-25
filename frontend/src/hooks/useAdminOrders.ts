import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminOrder } from '../lib/adminDesk'
import { ADMIN_PAGE_SIZE, pageQuery, type PageResult } from '../lib/page'

export const adminOrdersKey = ['admin', 'orders'] as const

export type OrderDesk = {
  todayCount: number
  todayTotalPaise: number
  confirmedCount: number
  confirmed: AdminOrder[]
  today: AdminOrder[]
}

export function useAdminOrders(page: number) {
  return useQuery({
    queryKey: [...adminOrdersKey, page],
    queryFn: () => api.get<PageResult<AdminOrder>>(`/admin/orders${pageQuery(page, ADMIN_PAGE_SIZE)}`)
  })
}

export function useOrderDesk() {
  return useQuery({
    queryKey: ['admin', 'desk', 'orders'],
    queryFn: () => api.get<OrderDesk>('/admin/orders/summary')
  })
}

export function useAdvanceOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderNumber, status }: { orderNumber: string; status: string }) =>
      api.patch<AdminOrder>(`/admin/orders/${orderNumber}`, { status }),
    onSuccess: (updated) => {
      queryClient.setQueriesData<PageResult<AdminOrder>>({ queryKey: adminOrdersKey }, (current) => {
        if (!current?.items) return current
        return {
          ...current,
          items: current.items.map((order) => (order.orderNumber === updated.orderNumber ? updated : order))
        }
      })
      queryClient.invalidateQueries({ queryKey: ['admin', 'desk', 'orders'] })
    }
  })
}
