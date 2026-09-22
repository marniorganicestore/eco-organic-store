const AUTH_PATHS = new Set(['/login', '/register', '/forgot-password'])

export function postLoginPath(from: unknown, roles: string[]): string {
  if (typeof from === 'string' && isSafeInternalPath(from)) {
    return from
  }
  return roles.includes('ADMIN') ? '/admin' : '/shop'
}

function isSafeInternalPath(path: string): boolean {
  if (!path.startsWith('/') || path.startsWith('//') || path.startsWith('/\\')) return false
  const pathname = path.split(/[?#]/, 1)[0] ?? path
  return !AUTH_PATHS.has(pathname)
}
