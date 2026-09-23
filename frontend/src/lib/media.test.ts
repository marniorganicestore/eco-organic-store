import { describe, expect, it } from 'vitest'
import { fileIssue, imageAddressIssue, resolveImageSrc } from './media'

describe('catalog photos', () => {
  it('keeps external addresses and points stored photos at the API host', () => {
    expect(resolveImageSrc('https://images.example/spinach.jpg', '/api')).toBe('https://images.example/spinach.jpg')
    expect(resolveImageSrc('/api/media/photo-1', '/api')).toBe('/api/media/photo-1')
    expect(resolveImageSrc('/api/media/photo-1', 'https://api.eco-organic-store.com/api')).toBe(
      'https://api.eco-organic-store.com/api/media/photo-1'
    )
  })

  it('accepts https addresses and rejects other schemes', () => {
    expect(imageAddressIssue('https://images.example/spinach.jpg')).toBeNull()
    expect(imageAddressIssue('/api/media/photo-1')).toBeNull()
    expect(imageAddressIssue('javascript:alert(1)')).toBe('Use an https image address.')
  })

  it('rejects a file that is too large or not a photo', () => {
    expect(fileIssue(new File([new Uint8Array(10)], 'notes.txt', { type: 'text/plain' }))).toBe('Use a JPEG, PNG, or WebP photo.')
    expect(fileIssue(new File([new Uint8Array(1_572_865)], 'big.jpg', { type: 'image/jpeg' }))).toBe('Use a photo under 1.5 MB.')
  })
})
