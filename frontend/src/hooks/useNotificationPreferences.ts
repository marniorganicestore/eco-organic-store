import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificationApi, type NotificationPreferences } from '../lib/notifications'

export const notificationQueryKey = ['notification-preferences'] as const

export function useNotificationPreferences() {
  return useQuery({
    queryKey: notificationQueryKey,
    queryFn: notificationApi.get
  })
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationApi.update,
    onSuccess: (preferences: NotificationPreferences) => {
      queryClient.setQueryData(notificationQueryKey, preferences)
    }
  })
}
