import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { ADMIN_PAGE_SIZE, pageQuery, type PageResult } from '../lib/page'

export type AdminAccount = {
  userId: string
  email: string
  name: string
  avatar?: string | null
  roles: string[]
  enabled: boolean
}

export const adminUsersQueryKey = ['admin', 'users'] as const

type AccessUpdate = {
  userId: string
  roles?: string[]
  enabled?: boolean
}

export function useAdminAccounts(page: number) {
  return useQuery({
    queryKey: [...adminUsersQueryKey, page],
    queryFn: () => api.get<PageResult<AdminAccount>>(`/admin/users${pageQuery(page, ADMIN_PAGE_SIZE)}`)
  })
}

export function useUpdateAccountAccess() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, roles, enabled }: AccessUpdate) =>
      api.patch<AdminAccount>(`/admin/users/${userId}`, { roles, enabled }),
    onSuccess: (updated) => {
      queryClient.setQueriesData<PageResult<AdminAccount>>({ queryKey: adminUsersQueryKey }, (current) => {
        if (!current?.items) return current
        return {
          ...current,
          items: current.items.map((account) => (account.userId === updated.userId ? updated : account))
        }
      })
    }
  })
}
