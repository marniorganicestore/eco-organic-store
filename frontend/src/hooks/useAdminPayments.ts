import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { AdminPayment } from '../lib/adminDesk'

export const adminPaymentsKey = ['admin', 'payments'] as const

export function useAdminPayments() {
  return useQuery({
    queryKey: adminPaymentsKey,
    queryFn: () => api.get<AdminPayment[]>('/admin/payments')
  })
}
