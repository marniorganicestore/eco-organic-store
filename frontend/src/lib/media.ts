import { joinApiPath, resolveApiBase } from './api'

const API_BASE = resolveApiBase(import.meta.env.VITE_API_BASE)

export const MAX_IMAGE_BYTES = 1_572_864

export type MediaUpload = {
  id: string
  url: string
  contentType: string
  size: number
}

export function resolveImageSrc(src: string | undefined | null, base = API_BASE): string | undefined {
  if (!src) return undefined
  if (!src.startsWith('/api/')) return src
  if (base === '/api') return src
  return joinApiPath(src.slice('/api'.length), base)
}

export function imageAddressIssue(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return 'Paste an image address.'
  if (trimmed.startsWith('/api/media/') && !trimmed.includes('..')) return null
  try {
    const url = new URL(trimmed)
    if (url.protocol !== 'https:' && url.protocol !== 'http:') return 'Use an https image address.'
    return null
  } catch {
    return 'Use an https image address.'
  }
}

export function fileIssue(file: File): string | null {
  if (file.size === 0) return 'Choose a photo to upload.'
  if (file.size > MAX_IMAGE_BYTES) return 'Use a photo under 1.5 MB.'
  const typed = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp'
  const named = /\.(jpe?g|png|webp)$/i.test(file.name)
  if (!typed && !named) return 'Use a JPEG, PNG, or WebP photo.'
  return null
}
