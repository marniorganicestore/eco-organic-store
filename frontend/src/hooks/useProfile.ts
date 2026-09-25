import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { profileApi, syncSessionUser, type AddressInput, type Profile } from '../lib/profile'

export const profileQueryKey = ['profile'] as const

function cacheProfile(queryClient: ReturnType<typeof useQueryClient>, profile: Profile) {
  queryClient.setQueryData(profileQueryKey, profile)
  syncSessionUser(profile)
}

export function useProfile() {
  return useQuery({
    queryKey: profileQueryKey,
    queryFn: async () => {
      const profile = await profileApi.get()
      syncSessionUser(profile)
      return profile
    }
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.update,
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}

export function useUploadAvatar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.uploadAvatar,
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}

export function useRemoveAvatar() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.removeAvatar,
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}

export function useAvatarLink() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.useAvatarLink,
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}

export function useSaveAddress(addressId?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: AddressInput) =>
      addressId ? profileApi.updateAddress(addressId, body) : profileApi.addAddress(body),
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}

export function useRemoveAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.removeAddress,
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}

export function useMakeDefaultAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: profileApi.makeDefault,
    onSuccess: (profile) => cacheProfile(queryClient, profile)
  })
}
