export function firstName(name: string): string {
  const trimmed = name.trim()
  return trimmed.split(/\s+/)[0] || name
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return 'H'
  if (parts.length === 1) return parts[0].slice(0, 1).toUpperCase()
  return `${parts[0].slice(0, 1)}${parts[1].slice(0, 1)}`.toUpperCase()
}

export const CUSTOMER_ROLE = 'CUSTOMER'
export const ADMIN_ROLE = 'ADMIN'

export function isAdmin(roles: string[] | undefined): boolean {
  return roles?.includes(ADMIN_ROLE) ?? false
}

export function roleLabel(role: string): string {
  if (role === ADMIN_ROLE) return 'Admin'
  if (role === CUSTOMER_ROLE) return 'Customer'
  return role
}
