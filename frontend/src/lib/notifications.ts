import { api } from './api'

export type NotificationPreferences = {
  orderUpdates: boolean
  email: string
  fromAddress: string
}

export const notificationApi = {
  get: () => api.get<NotificationPreferences>('/me/notifications'),
  update: (orderUpdates: boolean) => api.put<NotificationPreferences>('/me/notifications', { orderUpdates })
}
