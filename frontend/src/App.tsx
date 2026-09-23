import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError, authApi, api } from './lib/api'
import { RequireAdmin, RequireAuth } from './components/auth/RequireAuth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import { useAuthStore } from './store/authStore'
import { useCartStore } from './store/cartStore'
import { useCart } from './hooks/useCart'
import { checkoutBlocker } from './lib/cart'
import { AddToCartControl } from './components/cart/AddToCartControl'
import { CartNotice } from './components/cart/CartNotice'
import { CartSummary } from './components/cart/CartSummary'
import CartPage from './pages/CartPage'
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
import { STORE_NAME } from './lib/brand'
import { firstName, isAdmin } from './lib/userDisplay'
import { AdminLayout } from './components/admin/AdminLayout'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminCatalogPage from './pages/admin/AdminCatalogPage'
import AdminInventoryPage from './pages/admin/AdminInventoryPage'
import AdminOrdersPage from './pages/admin/AdminOrdersPage'
import AdminPaymentsPage from './pages/admin/AdminPaymentsPage'
import AdminReviewsPage from './pages/admin/AdminReviewsPage'
import AdminPeoplePage from './pages/admin/AdminPeoplePage'
import { storeBtn, storeBtnGhost, storeCard, storeInput, PageShell } from './components/layout/PageShell'
import { resolveImageSrc } from './lib/media'

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

function Layout({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((state) => state.user)
  const items = useCartStore((state) => state.items)
  useCart()
  const qty = items.reduce((sum, item) => sum + item.qty, 0)
  const showAdmin = isAdmin(user?.roles)
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const isScenePage = ['/', '/login', '/register', '/forgot-password'].includes(location.pathname)

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      useCartStore.getState().setItems([])
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
            src="/images/home-hero.png"
            alt=""
            aria-hidden="true"
            className="fixed inset-0 -z-20 h-full w-full object-cover"
          />
          <div className="fixed inset-0 -z-10 bg-[#f8f6f1]/86" />
        </>
      )}
      <header className="sticky top-0 z-20 border-b border-white/25 bg-[#f8f6f1]/78 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <Link to="/" className="text-2xl font-semibold text-emerald-900">{STORE_NAME}</Link>
          <nav className="flex items-center gap-4 text-sm">
            <Link className={navClass('/shop')} to="/shop">Shop</Link>
            <Link className={navClass('/account/orders')} to="/account/orders">Orders</Link>
            {showAdmin ? <Link className={navClass('/admin')} to="/admin">Admin</Link> : null}
            <Link className={navClass('/cart')} to="/cart" aria-label={`Cart, ${qty} ${qty === 1 ? 'item' : 'items'}`}>
              Cart
              <span className="ml-1.5 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-emerald-700 px-1.5 text-xs font-medium text-white">
                {qty}
              </span>
            </Link>
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
                  className={storeBtnGhost}
                  onClick={logout}
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link to="/login" className={`${storeBtn} px-3 py-1.5`}>
                Login
              </Link>
            )}
          </nav>
        </div>
      </header>
      <main className={isScenePage ? 'relative' : 'relative mx-auto max-w-6xl px-4 py-8'}>{children}</main>
      <CartNotice />
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
        <Route path="/admin" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="catalog" element={<AdminCatalogPage />} />
          <Route path="inventory" element={<AdminInventoryPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="payments" element={<AdminPaymentsPage />} />
          <Route path="reviews" element={<AdminReviewsPage />} />
          <Route path="people" element={<AdminPeoplePage />} />
        </Route>
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
          className={`${storeInput} w-56`}
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
        <p className={`${storeCard} p-8 text-sm text-slate-600`}>No products in this category yet.</p>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {products.map((p) => <ProductCard key={p.id} p={p} />)}
        </div>
      )}
    </PageShell>
  )
}

function ProductCard({ p }: { p: Product }) {
  return (
    <article className={`${storeCard} overflow-hidden p-4`}>
      <Link to={`/product/${p.slug}`}>
        <img className="mb-3 h-44 w-full rounded-xl object-cover" src={resolveImageSrc(p.images?.[0])} alt={p.name} />
        <h3 className="font-medium text-emerald-950">{p.name}</h3>
      </Link>
      <p className="text-sm text-slate-500">{p.unit} • {p.origin}</p>
      <p className="mt-1 font-semibold text-emerald-900">₹{(p.pricePaise / 100).toFixed(2)}</p>
      <AddToCartControl productId={p.id} productName={p.name} layout="card" />
    </article>
  )
}

function ProductPage() {
  const { slug } = useParams()
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

  return (
    <PageShell title={product.name} subtitle={`${product.unit} • ${product.origin}`}>
      <section className="grid gap-8 md:grid-cols-2">
        <img src={resolveImageSrc(product.images?.[0])} alt={product.name} className="h-96 w-full rounded-2xl object-cover shadow-lg" />
        <div className={`${storeCard} p-6`}>
          <p className="text-slate-600">{product.description}</p>
          <p className="my-4 text-2xl font-semibold text-emerald-950">₹{(product.pricePaise / 100).toFixed(2)}</p>
          <AddToCartControl productId={current.id} productName={current.name} layout="detail" />
        </div>
        <div className={`${storeCard} p-6 md:col-span-2`}>
          <h3 className="mb-3 text-lg font-semibold text-emerald-950">Reviews</h3>
          {reviews.length === 0
            ? <p className="text-sm text-slate-500">No reviews yet.</p>
            : reviews.map((r) => <p key={r.id} className="mb-2 text-sm">{r.rating}★ {r.body}</p>)}
        </div>
      </section>
    </PageShell>
  )
}

function CheckoutPage() {
  const address = useCheckoutAddress()
  const cartQuery = useCart()
  const blocker = cartQuery.data ? checkoutBlocker(cartQuery.data) : null
  const ready = Boolean(cartQuery.data && blocker == null)
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  async function pay() {
    if (!ready) {
      setError(blocker ?? 'Your basket is not ready for payment.')
      return
    }
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
    <PageShell title="Checkout" subtitle="Confirm the basket and where we should send it.">
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div className={`${storeCard} max-w-xl space-y-4 p-6`}>
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
          <button className={`${storeBtn} w-full`} type="button" disabled={pending || address.loading || cartQuery.isPending || !ready} aria-busy={pending} onClick={pay}>
            {pending ? 'Starting payment...' : 'Continue to payment'}
          </button>
        </div>
        <CartSummary cart={cartQuery.data} loading={cartQuery.isPending} showLines showCheckout={false} />
      </div>
    </PageShell>
  )
}

function OrderSuccess() {
  return (
    <PageShell title="Thank you">
      <div className={`${storeCard} p-8 text-emerald-950`}>
        Order payment completed. Thank you for choosing {STORE_NAME}.
      </div>
    </PageShell>
  )
}
