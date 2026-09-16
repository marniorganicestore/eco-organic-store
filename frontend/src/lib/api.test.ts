import { describe, expect, it } from 'vitest'
import { joinApiPath, resolveApiBase } from './api'

describe('resolveApiBase', () => {
  it('defaults blank values to the Vite/gateway proxy prefix', () => {
    expect(resolveApiBase(undefined)).toBe('/api')
    expect(resolveApiBase('')).toBe('/api')
    expect(resolveApiBase('   ')).toBe('/api')
  })

  it('keeps an explicit /api prefix and appends it to a host', () => {
    expect(resolveApiBase('/api')).toBe('/api')
    expect(resolveApiBase('https://api.harvest.test/api/')).toBe('https://api.harvest.test/api')
    expect(resolveApiBase('https://api.harvest.test')).toBe('https://api.harvest.test/api')
  })
})

describe('joinApiPath', () => {
  it('builds auth URLs under /api so static hosts are not POSTed at /auth/register', () => {
    expect(joinApiPath('/auth/register')).toBe('/api/auth/register')
    expect(joinApiPath('/auth/register', '/api')).toBe('/api/auth/register')
    expect(joinApiPath('auth/login', '/api')).toBe('/api/auth/login')
    expect(joinApiPath('/auth/register', 'https://api.harvest.test/api')).toBe(
      'https://api.harvest.test/api/auth/register'
    )
  })
})
