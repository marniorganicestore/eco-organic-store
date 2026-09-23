import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError, authApi, api } from './lib/api'
import { RequireAdmin, RequireAuth } from './components/auth/RequireAuth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import { useAuthStore } from './store/authStore'
import { useCartStore } from './store/cartStore'
import { HomeHero } from './components/home/HomeHero'
import { AccountLayout } from './components/account/AccountLayout'
import { UserAvatar } from './components/account/UserAvatar'
import { ShippingAddressPicker } from './components/checkout/ShippingAddressPicker'
import { useCheckoutAddress } from './hooks/useCheckoutAddress'
import { FormBanner } from './components/account/FormBanner'
import ProfilePage from './pages/account/ProfilePage'
import AddressesPage from './pages/account/AddressesPage'
import SecurityPage from './pages/account/SecurityPage'
import OrdersPage from './pages/account/OrdersPage'
import { firstName, isAdmin } from './lib/userDisplay'
import { AdminUsersPanel } from './components/admin/AdminUsersPanel'
import { harvestBtn, harvestBtnGhost, harvestCard, harvestInput, PageShell } from './components/layout/PageShell'

type Product = {
  id: string
  slug: string
  name: string
  description: string
  pricePaise: number
  images: string[]
  origin: string
  certifications: string[]
  unit: string
  featured: boolean
  averageRating: number
  reviewCount: number
}

type Category = { id: string; slug: string; name: string }

type CartItem = { productId: string; qty: number }

function Layout({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((state) => state.user)
  const { items } = useCartStore()
  const qty = items.reduce((sum, i) => sum + i.qty, 0)
  const showAdmin = isAdmin(user?.roles)
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const isScenePage = ['/', '/login', '/register', '/forgot-password'].includes(location.pathname)

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      queryClient.clear()
      navigate('/login', { replace: true })
    }
  }

  function navClass(path: string) {
    const active = path === '/' ? location.pathname === '/' : location.pathname.startsWith(path)
    return active
      ? 'font-medium text-emerald-950'
      : 'text-emerald-900/70 hover:text-emerald-950'
  }

  return (
    <div className="relative min-h-screen text-slate-800">
      {isScenePage ? (
        <div className="absolute inset-0 -z-10 bg-emerald-950" />
      ) : (
        <>
          <img
            src="/images/home-harvest.png"
            alt=""
            aria-hidden="true"
            className="fixed inset-0 -z-20 h-full w-full object-cover"
          />
          <div className="fixed inset-0 -z-10 bg-[#f8f6f1]/86" />
        </>
      )}
      <header className="sticky top-0 z-20 border-b border-white/25 bg-[#f8f6f1]/78 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <Link to="/" className="text-2xl font-semibold text-emerald-900">Harvest & Co.</Link>
          <nav className="flex items-center gap-4 text-sm">
            <Link className={navClass('/shop')} to="/shop">Shop</Link>
            <Link className={navClass('/account/orders')} to="/account/orders">Orders</Link>
            {showAdmin ? <Link className={navClass('/admin')} to="/admin">Admin</Link> : null}
            <Link className={navClass('/cart')} to="/cart">Cart ({qty})</Link>
            {user ? (
              <div className="flex items-center gap-2">
                <Link
                  to="/account"
                  className="flex items-center gap-2 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
                  aria-label={`Account for ${user.name || user.email}`}
                >
                  <UserAvatar name={user.name || user.email} avatar={user.avatar} />
                  <span className="hidden max-w-28 truncate text-emerald-900 sm:inline">{firstName(user.name || user.email)}</span>
                </Link>
                {showAdmin ? (
                  <span className="hidden rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-900 sm:inline">
                    Admin
                  </span>
                ) : null}
                <button
                  type="button"
                  className={harvestBtnGhost}
                  onClick={logout}
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link to="/login" className={`${harvestBtn} px-3 py-1.5`}>
                Login
              </Link>
            )}
          </nav>
        </div>
      </header>
      <main className={isScenePage ? 'relative' : 'relative mx-auto max-w-6xl px-4 py-8'}>{children}</main>
    </div>
  )
}

export default function App() {
  const bootstrapped = useAuthStore((state) => state.bootstrapped)

  useEffect(() => {
    localStorage.setItem('guestToken', useCartStore.getState().guestToken)
    authApi.bootstrapSession()
  }, [])

  if (!bootstrapped) return <Layout><p className="p-6 text-sm text-emerald-900/80">Restoring session...</p></Layout>

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/product/:slug" element={<ProductPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<RequireAuth><CheckoutPage /></RequireAuth>} />
        <Route path="/order/success" element={<OrderSuccess />} />
        <Route path="/account" element={<RequireAuth><AccountLayout /></RequireAuth>}>
          <Route index element={<ProfilePage />} />
          <Route path="addresses" element={<AddressesPage />} />
          <Route path="security" element={<SecurityPage />} />
          <Route path="orders" element={<OrdersPage />} />
        </Route>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/admin" element={<RequireAdmin><Admin /></RequireAdmin>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}

function Home() {
  return <HomeHero />
}

function Shop() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [category, setCategory] = useState('')
  const [loadError, setLoadError] = useState<string>('')

  useEffect(() => {
    api.get<Category[]>('/categories')
      .then((data) => {
        setCategories(data)
        setLoadError('')
      })
      .catch(() => {
        setCategories([])
        setLoadError('Unable to load categories. Verify gateway/API base is reachable.')
      })
  }, [])
  useEffect(() => {
    const q = category ? `?category=${category}` : ''
    api.get<Product[]>(`/products${q}`)
      .then((data) => {
        setProducts(data)
        setLoadError('')
      })
      .catch(() => {
        setProducts([])
        setLoadError('Unable to load products. Verify gateway/API base is reachable.')
      })
  }, [category])

  return (
    <PageShell
      title="Shop"
      subtitle="Seasonal organic produce and pantry staples from trusted farms."
      actions={(
        <select
          className={`${harvestInput} w-56`}
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          aria-label="Filter by category"
        >
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.slug}>{c.name}</option>)}
        </select>
      )}
    >
      {loadError ? (
        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50/90 px-3 py-2 text-sm text-amber-900">
          {loadError}
        </div>
      ) : null}
      {products.length === 0 && !loadError ? (
        <p className={`${harvestCard} p-8 text-sm text-slate-600`}>No products in this category yet.</p>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {products.map((p) => <ProductCard key={p.id} p={p} />)}
        </div>
      )}
    </PageShell>
  )
}

function ProductCard({ p }: { p: Product }) {
  const { guestToken, setItems } = useCartStore()
  async function add() {
    const cart = await api.post<{ items: CartItem[] }>(`/cart?guestToken=${guestToken}`, { productId: p.id, qty: 1 })
    setItems(cart.items)
  }
  return (
    <article className={`${harvestCard} overflow-hidden p-4`}>
      <Link to={`/product/${p.slug}`}>
        <img className="mb-3 h-44 w-full rounded-xl object-cover" src={p.images?.[0]} alt={p.name} />
        <h3 className="font-medium text-emerald-950">{p.name}</h3>
      </Link>
      <p className="text-sm text-slate-500">{p.unit} • {p.origin}</p>
      <p className="mt-1 font-semibold text-emerald-900">₹{(p.pricePaise / 100).toFixed(2)}</p>
      <button type="button" onClick={add} className={`${harvestBtn} mt-3 w-full`}>Add to cart</button>
    </article>
  )
}

function ProductPage() {
  const { slug } = useParams()
  const { guestToken, setItems } = useCartStore()
  const [product, setProduct] = useState<Product | null>(null)
  const [reviews, setReviews] = useState<any[]>([])
  useEffect(() => {
    api.get<Product>(`/products/${slug}`).then(setProduct)
  }, [slug])
  useEffect(() => {
    if (product) api.get<any[]>(`/products/${product.id}/reviews`).then(setReviews)
  }, [product])
  if (!product) return <p className="text-sm text-emerald-900/80">Loading...</p>
  const current = product

  async function add() {
    const cart = await api.post<{ items: CartItem[] }>(`/cart?guestToken=${guestToken}`, { productId: current.id, qty: 1 })
    setItems(cart.items)
  }

  return (
    <PageShell title={product.name} subtitle={`${product.unit} • ${product.origin}`}>
      <section className="grid gap-8 md:grid-cols-2">
        <img src={product.images?.[0]} alt={product.name} className="h-96 w-full rounded-2xl object-cover shadow-lg" />
        <div className={`${harvestCard} p-6`}>
          <p className="text-slate-600">{product.description}</p>
          <p className="my-4 text-2xl font-semibold text-emerald-950">₹{(product.pricePaise / 100).toFixed(2)}</p>
          <button type="button" onClick={add} className={`${harvestBtn} w-full`}>Add to cart</button>
        </div>
        <div className={`${harvestCard} p-6 md:col-span-2`}>
          <h3 className="mb-3 text-lg font-semibold text-emerald-950">Reviews</h3>
          {reviews.length === 0
            ? <p className="text-sm text-slate-500">No reviews yet.</p>
            : reviews.map((r) => <p key={r.id} className="mb-2 text-sm">{r.rating}★ {r.body}</p>)}
        </div>
      </section>
    </PageShell>
  )
}

function CartPage() {
  const { guestToken, items, setItems } = useCartStore()
  const navigate = useNavigate()
  useEffect(() => {
    api.get<{ items: CartItem[] }>(`/cart?guestToken=${guestToken}`).then((c) => setItems(c.items || []))
  }, [guestToken, setItems])

  const totalItems = useMemo(() => items.reduce((sum, i) => sum + i.qty, 0), [items])

  return (
    <PageShell title="Cart" subtitle="Review your harvest before checkout.">
      <div className={`${harvestCard} p-6`}>
        {items.length === 0 ? <p className="text-slate-500">Your cart is empty.</p> : null}
        <ul className="space-y-3">
          {items.map((i) => (
            <li key={i.productId} className="rounded-xl border border-emerald-100 bg-white/70 p-3 text-sm text-emerald-950">
              {i.productId} × {i.qty}
            </li>
          ))}
        </ul>
        <button
          disabled={!totalItems}
          className={`${harvestBtn} mt-5`}
          onClick={() => navigate('/checkout')}
        >
          Proceed to checkout
        </button>
      </div>
    </PageShell>
  )
}

function CheckoutPage() {
  const address = useCheckoutAddress()
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  async function pay() {
    if (!address.shippingAddress.trim()) {
      setError('Add a delivery address to continue.')
      return
    }
    setError('')
    setPending(true)
    try {
      const res = await api.post<{ checkoutUrl: string }>('/checkout/sessions', { shippingAddress: address.shippingAddress.trim() })
      window.location.href = res.checkoutUrl
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to start payment. Please try again.')
      setPending(false)
    }
  }

  return (
    <PageShell title="Checkout" subtitle="Where should we send this harvest?">
      <div className={`${harvestCard} max-w-xl space-y-4 p-6`}>
        {error ? <FormBanner tone="error">{error}</FormBanner> : null}
        <ShippingAddressPicker
          loading={address.loading}
          deliverable={address.deliverable}
          needsDetails={address.needsDetails}
          selectedId={address.selectedId}
          customValue={address.customValue}
          onSelect={address.select}
          onCustomChange={address.setCustomValue}
        />
        <button className={`${harvestBtn} w-full`} type="button" disabled={pending || address.loading} aria-busy={pending} onClick={pay}>
          {pending ? 'Starting payment...' : 'Continue to payment'}
        </button>
      </div>
    </PageShell>
  )
}

function OrderSuccess() {
  return (
    <PageShell title="Thank you">
      <div className={`${harvestCard} p-8 text-emerald-950`}>
        Order payment completed. Thank you for choosing Harvest &amp; Co.
      </div>
    </PageShell>
  )
}

function Admin() {
  const [lowStock, setLowStock] = useState<any[]>([])
  const [payments, setPayments] = useState<any[]>([])
  const [products, setProducts] = useState<any[]>([])
  const [orders, setOrders] = useState<any[]>([])
  const [hiddenReviews, setHiddenReviews] = useState<any[]>([])
  const [adjustQty, setAdjustQty] = useState<Record<string, number>>({})

  useEffect(() => {
    api.get<any[]>('/admin/inventory/low-stock').then(setLowStock).catch(() => setLowStock([]))
    api.get<any[]>('/admin/payments').then(setPayments).catch(() => setPayments([]))
    api.get<any[]>('/admin/catalog/products').then(setProducts).catch(() => setProducts([]))
    api.get<any[]>('/admin/orders').then(setOrders).catch(() => setOrders([]))
    api.get<any[]>('/admin/reviews').then(setHiddenReviews).catch(() => setHiddenReviews([]))
  }, [])

  async function updateOrderStatus(orderNumber: string, status: string) {
    await api.patch(`/admin/orders/${orderNumber}`, { status })
    setOrders((prev) => prev.map((o) => o.orderNumber === orderNumber ? { ...o, orderStatus: status } : o))
  }

  async function adjustStock(productId: string) {
    const onHand = adjustQty[productId] ?? 0
    await api.patch(`/admin/inventory/${productId}`, { onHand })
  }

  async function reviewVisible(reviewId: string) {
    await api.patch(`/admin/reviews/${reviewId}`, { status: 'VISIBLE' })
    setHiddenReviews((prev) => prev.filter((r) => r.id !== reviewId))
  }

  return (
    <PageShell title="Admin" subtitle="Inventory, payments, orders, and reviews.">
      <div className="grid gap-4 md:grid-cols-2">
        <AdminUsersPanel />
        <section className={`${harvestCard} p-4`}>
          <h3 className="mb-2 font-semibold text-emerald-950">Low stock</h3>
          {lowStock.map((s) => <p key={s.productId}>{s.productId}: {s.available}</p>)}
        </section>
        <section className={`${harvestCard} p-4`}>
          <h3 className="mb-2 font-semibold text-emerald-950">Payments</h3>
          {payments.map((p) => <p key={p.id}>{p.orderNumber}: {p.status}</p>)}
        </section>
        <section className={`${harvestCard} p-4 md:col-span-2`}>
          <h3 className="mb-2 font-semibold text-emerald-950">Products and inventory</h3>
          {products.map((p) => (
            <div key={p.id} className="mb-2 flex items-center gap-2 rounded-xl border border-emerald-100 bg-white/70 p-2">
              <span className="min-w-60 text-sm">{p.name}</span>
              <input type="number" className={`${harvestInput} w-24`} placeholder="on hand" onChange={(e) => setAdjustQty((prev) => ({ ...prev, [p.id]: Number(e.target.value) }))} />
              <button className={harvestBtn} onClick={() => adjustStock(p.id)}>Update stock</button>
            </div>
          ))}
        </section>
        <section className={`${harvestCard} p-4 md:col-span-2`}>
          <h3 className="mb-2 font-semibold text-emerald-950">Orders</h3>
          {orders.map((o) => (
            <div key={o.id} className="mb-2 flex items-center gap-2 rounded-xl border border-emerald-100 bg-white/70 p-2">
              <span className="min-w-56 text-sm">{o.orderNumber}</span>
              <span className="min-w-32 text-sm">{o.orderStatus}</span>
              <button className={harvestBtnGhost} onClick={() => updateOrderStatus(o.orderNumber, 'PACKED')}>Pack</button>
              <button className={harvestBtnGhost} onClick={() => updateOrderStatus(o.orderNumber, 'SHIPPED')}>Ship</button>
              <button className={harvestBtnGhost} onClick={() => updateOrderStatus(o.orderNumber, 'DELIVERED')}>Deliver</button>
            </div>
          ))}
        </section>
        <section className={`${harvestCard} p-4 md:col-span-2`}>
          <h3 className="mb-2 font-semibold text-emerald-950">Hidden reviews</h3>
          {hiddenReviews.length === 0 ? <p className="text-sm text-slate-500">No hidden reviews.</p> : hiddenReviews.map((r) => (
            <div key={r.id} className="mb-2 flex items-center justify-between rounded-xl border border-emerald-100 bg-white/70 p-2">
              <p>{r.productId}: {r.body}</p>
              <button className={harvestBtn} onClick={() => reviewVisible(r.id)}>Make visible</button>
            </div>
          ))}
        </section>
      </div>
    </PageShell>
  )
}
