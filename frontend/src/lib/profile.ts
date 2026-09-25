import { api } from './api'
import { INDIAN_STATES } from './india'
import { useAuthStore } from '../store/authStore'

const PIN = /^[1-9][0-9]{5}$/
const PHONE = /^(?:\+91[\s-]?)?[6-9]\d{9}$/

export type Address = {
  id: string
  label: string
  recipient: string
  line1: string
  line2: string
  city: string
  state: string
  postalCode: string
  phone: string
  defaultAddress: boolean
}

export type AddressInput = {
  label: string
  recipient: string
  line1: string
  line2: string
  city: string
  state: string
  postalCode: string
  phone: string
  defaultAddress: boolean
}

export type Profile = {
  userId: string
  email: string
  name: string
  avatar: string | null
  phone: string | null
  roles: string[]
  passwordSet: boolean
  googleLinked: boolean
  addresses: Address[]
}

export const emptyAddress: AddressInput = {
  label: 'Home',
  recipient: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  postalCode: '',
  phone: '',
  defaultAddress: false
}

export function formatAddress(address: Pick<Address, 'recipient' | 'line1' | 'line2' | 'city' | 'state' | 'postalCode' | 'phone'>): string {
  const locality = [address.city.trim(), address.state.trim()].filter(Boolean).join(', ')
  const localityPin = [locality, address.postalCode.trim()].filter(Boolean).join(' ')
  return [address.recipient, address.line1, address.line2, localityPin, address.phone]
    .map((part) => part.trim())
    .filter(Boolean)
    .join('\n')
}

export function isDeliverableAddress(address: Address): boolean {
  return Boolean(
    address.recipient.trim()
    && address.line1.trim()
    && address.city.trim()
    && INDIAN_STATES.includes(address.state as (typeof INDIAN_STATES)[number])
    && PIN.test(address.postalCode.trim())
  )
}

const OWNED_AVATAR = /^\/api\/avatars\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export function validateProfile(input: { name: string; phone: string; avatar?: string }): string | null {
  const name = input.name.trim()
  if (name.length < 2 || name.length > 80) return 'Name must be 2–80 characters.'
  const phone = input.phone.trim()
  if (phone && !PHONE.test(phone)) return 'Enter a valid Indian mobile number.'
  const avatar = input.avatar?.trim() ?? ''
  if (avatar) {
    if (OWNED_AVATAR.test(avatar)) return null
    if (avatar.length > 1000) return 'Avatar link is too long.'
    try {
      const url = new URL(avatar)
      if (url.protocol !== 'https:' && url.protocol !== 'http:') return 'Avatar must be an http(s) link.'
    } catch {
      return 'Avatar must be an http(s) link.'
    }
  }
  return null
}

export function validateAddress(input: AddressInput): string | null {
  if (input.label.trim().length < 1 || input.label.trim().length > 40) return 'Enter a short label, such as Home or Work.'
  if (input.recipient.trim().length < 2 || input.recipient.trim().length > 80) return 'Enter the recipient name.'
  if (input.line1.trim().length < 3 || input.line1.trim().length > 120) return 'Enter the street address.'
  if (input.line2.trim().length > 120) return 'Address line 2 is too long.'
  if (input.city.trim().length < 2 || input.city.trim().length > 60) return 'Enter the city.'
  if (!INDIAN_STATES.includes(input.state as (typeof INDIAN_STATES)[number])) return 'Choose a state or union territory.'
  if (!PIN.test(input.postalCode.trim())) return 'Enter a 6-digit PIN code.'
  if (input.phone.trim() && !PHONE.test(input.phone.trim())) return 'Enter a valid Indian mobile number.'
  return null
}

export function validatePasswordChange(currentPassword: string, newPassword: string, confirmPassword: string): string | null {
  if (!currentPassword) return 'Enter your current password.'
  if (newPassword.length < 8 || newPassword.length > 72) return 'Password must be 8–72 characters.'
  if (newPassword !== confirmPassword) return 'New password and confirmation do not match.'
  if (newPassword === currentPassword) return 'Choose a password that is different from your current one.'
  return null
}

export function syncSessionUser(profile: Profile): void {
  const current = useAuthStore.getState().user
  if (!current) return
  useAuthStore.getState().setUser({
    ...current,
    name: profile.name,
    email: profile.email,
    roles: profile.roles,
    avatar: profile.avatar
  })
}

export const profileApi = {
  get: () => api.get<Profile>('/me'),
  update: (body: { name: string; phone: string }) => api.patch<Profile>('/me', body),
  uploadAvatar: (file: File) => api.upload<Profile>('/me/avatar', file),
  removeAvatar: () => api.delete<Profile>('/me/avatar'),
  useAvatarLink: (avatar: string) => api.patch<Profile>('/me', { avatar }),
  addAddress: (body: AddressInput) => api.post<Profile>('/me/addresses', body),
  updateAddress: (id: string, body: AddressInput) => api.patch<Profile>(`/me/addresses/${encodeURIComponent(id)}`, body),
  removeAddress: (id: string) => api.delete<Profile>(`/me/addresses/${encodeURIComponent(id)}`),
  makeDefault: (id: string) => api.post<Profile>(`/me/addresses/${encodeURIComponent(id)}/default`)
}
