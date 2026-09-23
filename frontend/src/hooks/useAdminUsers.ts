import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'

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

export function useAdminAccounts() {
  return useQuery({
    queryKey: adminUsersQueryKey,
    queryFn: () => api.get<AdminAccount[]>('/admin/users')
  })
}

export function useUpdateAccountAccess() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ userId, roles, enabled }: AccessUpdate) =>
      api.patch<AdminAccount>(`/admin/users/${userId}`, { roles, enabled }),
    onSuccess: (updated) => {
      queryClient.setQueryData<AdminAccount[]>(adminUsersQueryKey, (current) =>
        current?.map((account) => (account.userId === updated.userId ? updated : account)) ?? [updated]
      )
    }
  })
}
