export const LOW_STOCK_AT = 10

export type AdminProduct = {
  id: string
  slug: string
  name: string
  description: string
  pricePaise: number
  images: string[]
  categoryId: string
  origin: string
  certifications: string[]
  unit: string
  active: boolean
  featured: boolean
  averageRating: number
  reviewCount: number
}

export type ProductWrite = {
  name: string
  slug: string
  description: string
  pricePaise: number
  images: string[]
  categoryId: string
  origin: string
  certifications: string[]
  unit: string
  featured: boolean
  active: boolean
}

export type ProductDraft = {
  name: string
  slug: string
  description: string
  priceRupees: string
  images: string[]
  categoryId: string
  origin: string
  certifications: string
  unit: string
  featured: boolean
  active: boolean
}

export type AdminCategory = {
  id: string
  slug: string
  name: string
  image: string
  sortOrder: number
}

export type CategoryWrite = {
  name: string
  slug: string
  image: string
  sortOrder: number
}

export type CategoryDraft = {
  name: string
  slug: string
  image: string
  sortOrder: string
}

export type AdminStock = {
  productId: string
  onHand: number
  reserved: number
  available: number
}

export type AdminOrderLine = {
  productId: string
  productName: string
  pricePaise: number
  qty: number
}

export type AdminOrder = {
  id: string
  orderNumber: string
  userId: string
  lines: AdminOrderLine[]
  shippingAddress: string
  totalPaise: number
  orderStatus: string
  createdAt: string
}

export type AdminPayment = {
  id: string
  orderNumber: string
  amountPaise: number
  status: string
  createdAt: string
}

export type AdminReview = {
  id: string
  userId: string
  productId: string
  rating: number
  body: string
  verifiedPurchase: boolean
  status: string
  createdAt: string
}

export function formatInr(paise: number): string {
  return `₹${(paise / 100).toFixed(2)}`
}

export function paiseToRupees(paise: number): string {
  return (paise / 100).toFixed(2)
}

export function rupeesToPaise(value: string): number | null {
  const trimmed = value.trim()
  if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) return null
  const [whole, fraction = ''] = trimmed.split('.')
  const paise = Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
  return Number.isSafeInteger(paise) ? paise : null
}

export function slugify(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

export function statusLabel(status: string): string {
  return status
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function nextFulfillment(status: string): { status: string; label: string } | null {
  switch (status) {
    case 'CONFIRMED':
      return { status: 'PACKED', label: 'Mark packed' }
    case 'PACKED':
      return { status: 'SHIPPED', label: 'Mark shipped' }
    case 'SHIPPED':
      return { status: 'DELIVERED', label: 'Mark delivered' }
    default:
      return null
  }
}

export function isSameLocalDay(iso: string, now = new Date()): boolean {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return false
  return date.toDateString() === now.toDateString()
}

export function formatWhen(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

export function stockForProduct(stocks: AdminStock[], productId: string): AdminStock {
  return stocks.find((stock) => stock.productId === productId) ?? {
    productId,
    onHand: 0,
    reserved: 0,
    available: 0
  }
}

export function isLowStock(stock: AdminStock): boolean {
  return stock.available <= LOW_STOCK_AT
}

export function splitList(value: string): string[] {
  return value
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
}

export function emptyProductDraft(categoryId = ''): ProductDraft {
  return {
    name: '',
    slug: '',
    description: '',
    priceRupees: '',
    images: [],
    categoryId,
    origin: '',
    certifications: '',
    unit: '',
    featured: false,
    active: true
  }
}

export function productToDraft(product: AdminProduct): ProductDraft {
  return {
    name: product.name,
    slug: product.slug,
    description: product.description ?? '',
    priceRupees: paiseToRupees(product.pricePaise),
    images: product.images ?? [],
    categoryId: product.categoryId,
    origin: product.origin ?? '',
    certifications: product.certifications.join(', '),
    unit: product.unit,
    featured: product.featured,
    active: product.active
  }
}

export function productIssue(draft: ProductDraft): string | null {
  if (!draft.name.trim()) return 'Enter a product name.'
  if (!slugify(draft.slug || draft.name)) return 'Use a name made of letters or numbers.'
  if (rupeesToPaise(draft.priceRupees) === null) return 'Enter a price in rupees, such as 179.00.'
  if (!draft.categoryId) return 'Choose a category.'
  if (!draft.unit.trim()) return 'Enter a unit, such as 500g.'
  return null
}

export function toProductWrite(draft: ProductDraft): ProductWrite {
  const pricePaise = rupeesToPaise(draft.priceRupees)
  if (pricePaise === null) throw new Error('Enter a price in rupees, such as 179.00.')
  return {
    name: draft.name.trim(),
    slug: slugify(draft.slug || draft.name),
    description: draft.description.trim(),
    pricePaise,
    images: draft.images.map((image) => image.trim()).filter(Boolean).slice(0, 6),
    categoryId: draft.categoryId,
    origin: draft.origin.trim(),
    certifications: splitList(draft.certifications),
    unit: draft.unit.trim(),
    featured: draft.featured,
    active: draft.active
  }
}

export function emptyCategoryDraft(): CategoryDraft {
  return { name: '', slug: '', image: '', sortOrder: '1' }
}

export function categoryToDraft(category: AdminCategory): CategoryDraft {
  return {
    name: category.name,
    slug: category.slug,
    image: category.image ?? '',
    sortOrder: String(category.sortOrder)
  }
}

export function categoryIssue(draft: CategoryDraft): string | null {
  if (!draft.name.trim()) return 'Enter a category name.'
  if (!slugify(draft.slug || draft.name)) return 'Use a name made of letters or numbers.'
  if (!/^\d+$/.test(draft.sortOrder.trim())) return 'Enter a sort order from 0 to 999.'
  const sortOrder = Number(draft.sortOrder)
  if (sortOrder > 999) return 'Enter a sort order from 0 to 999.'
  return null
}

export function toCategoryWrite(draft: CategoryDraft): CategoryWrite {
  return {
    name: draft.name.trim(),
    slug: slugify(draft.slug || draft.name),
    image: draft.image.trim(),
    sortOrder: Number(draft.sortOrder)
  }
}

export function onHandIssue(value: string): string | null {
  if (!/^\d+$/.test(value.trim())) return 'Enter a whole number of units on hand.'
  return null
}
