import { describe, expect, it } from 'vitest'
import { formatAddress, isDeliverableAddress, validateAddress, validatePasswordChange, validateProfile, type Address } from './profile'

const home: Address = {
  id: 'a1',
  label: 'Home',
  recipient: 'Asha Rao',
  line1: '12 Orchard Lane',
  line2: '',
  city: 'Pune',
  state: 'Maharashtra',
  postalCode: '411001',
  phone: '9876543210',
  defaultAddress: true
}

describe('profile helpers', () => {
  it('formats a delivery address as a checkout snapshot', () => {
    expect(formatAddress(home)).toBe('Asha Rao\n12 Orchard Lane\nPune, Maharashtra 411001\n9876543210')
  })

  it('rejects an incomplete profile and an unknown state', () => {
    expect(validateProfile({ name: 'A', phone: '', avatar: '' })).toBe('Name must be 2–80 characters.')
    expect(validateProfile({ name: 'Asha', phone: '12345', avatar: '' })).toBe('Enter a valid Indian mobile number.')
    expect(validateAddress({ ...home, state: 'Narnia' })).toBe('Choose a state or union territory.')
    expect(isDeliverableAddress({ ...home, postalCode: '000000' })).toBe(false)
  })

  it('requires a distinct confirmed password', () => {
    expect(validatePasswordChange('current-password', 'current-password', 'current-password'))
      .toBe('Choose a password that is different from your current one.')
    expect(validatePasswordChange('current-password', 'new-password-1', 'other')).toBe('New password and confirmation do not match.')
    expect(validatePasswordChange('current-password', 'new-password-1', 'new-password-1')).toBeNull()
  })
})
