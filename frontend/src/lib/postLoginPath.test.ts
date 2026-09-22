import { describe, expect, it } from 'vitest'
import { postLoginPath } from './postLoginPath'

describe('postLoginPath', () => {
  it('returns the original in-app path after login', () => {
    expect(postLoginPath('/checkout', ['CUSTOMER'])).toBe('/checkout')
    expect(postLoginPath('/account/orders?tab=open', ['CUSTOMER'])).toBe('/account/orders?tab=open')
  })

  it('rejects open redirects and auth pages', () => {
    expect(postLoginPath('//evil.example', ['CUSTOMER'])).toBe('/shop')
    expect(postLoginPath('/login', ['CUSTOMER'])).toBe('/shop')
    expect(postLoginPath('/register', ['ADMIN'])).toBe('/admin')
  })

  it('sends admins to the admin home when no return path exists', () => {
    expect(postLoginPath(undefined, ['ADMIN'])).toBe('/admin')
  })
})
