import { describe, expect, it } from 'vitest'
import {
  categoryIssue,
  formatInr,
  isSameLocalDay,
  nextFulfillment,
  productIssue,
  rupeesToPaise,
  slugify,
  toProductWrite,
  emptyProductDraft
} from './adminDesk'

describe('admin desk helpers', () => {
  it('turns rupees into paise without floating-point drift', () => {
    expect(rupeesToPaise('179.00')).toBe(17900)
    expect(rupeesToPaise('19.9')).toBe(1990)
    expect(rupeesToPaise('19.999')).toBeNull()
    expect(formatInr(17900)).toBe('₹179.00')
  })

  it('builds a product write from a draft and rejects a missing category', () => {
    const draft = {
      ...emptyProductDraft(),
      name: 'Farm Carrots',
      priceRupees: '99.00',
      unit: '500g',
      categoryId: 'cat-1',
      certifications: 'India Organic, '
    }
    expect(slugify('Farm Carrots')).toBe('farm-carrots')
    expect(productIssue(draft)).toBeNull()
    expect(toProductWrite(draft)).toMatchObject({
      slug: 'farm-carrots',
      pricePaise: 9900,
      certifications: ['India Organic'],
      active: true
    })
    expect(productIssue({ ...draft, categoryId: '' })).toBe('Choose a category.')
    expect(categoryIssue({ name: 'Produce', slug: '', image: '', sortOrder: '1000' })).toBe('Enter a sort order from 0 to 999.')
  })

  it('advances fulfillment one step and treats today in local time', () => {
    expect(nextFulfillment('CONFIRMED')).toEqual({ status: 'PACKED', label: 'Mark packed' })
    expect(nextFulfillment('PENDING_PAYMENT')).toBeNull()
    expect(nextFulfillment('DELIVERED')).toBeNull()
    const now = new Date('2026-09-23T18:00:00')
    expect(isSameLocalDay('2026-09-23T02:00:00', now)).toBe(true)
    expect(isSameLocalDay('2026-09-22T18:00:00', now)).toBe(false)
  })
})
