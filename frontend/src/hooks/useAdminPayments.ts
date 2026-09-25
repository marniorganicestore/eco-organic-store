import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminPayment } from '../lib/adminDesk'
import { ADMIN_PAGE_SIZE, pageQuery, type PageResult } from '../lib/page'

export function useAdminPayments(page: number) {
  return useQuery({
    queryKey: ['admin', 'payments', page],
    queryFn: () => api.get<PageResult<AdminPayment>>(`/admin/payments${pageQuery(page, ADMIN_PAGE_SIZE)}`)
  })
}

export function usePaymentDesk() {
  return useQuery({
    queryKey: ['admin', 'desk', 'payments'],
    queryFn: () => api.get<{ pendingCount: number }>('/admin/payments/summary')
  })
}
