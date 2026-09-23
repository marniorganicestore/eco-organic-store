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

export function roleLabel(role: string): string {
  if (role === 'ADMIN') return 'Admin'
  if (role === 'CUSTOMER') return 'Customer'
  return role
}
