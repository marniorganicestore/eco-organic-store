import { Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authApi, api } from './lib/api'
import { RequireAdmin, RequireAuth } from './components/auth/RequireAuth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import { useAuthStore } from './store/authStore'
import { useCartStore } from './store/cartStore'

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

function firstName(name: string): string {
  return name.trim().split(/\s+/)[0] || name
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return 'H'
  if (parts.length === 1) return parts[0].slice(0, 1).toUpperCase()
  return `${parts[0].slice(0, 1)}${parts[1].slice(0, 1)}`.toUpperCase()
}

function Layout({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((state) => state.user)
  const { items } = useCartStore()
  const qty = items.reduce((sum, i) => sum + i.qty, 0)
  const isAdmin = user?.roles.includes('ADMIN') ?? false
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      queryClient.clear()
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="min-h-screen bg-[#f8f6f1] text-slate-800">
      <header className="sticky top-0 z-20 border-b border-emerald-100 bg-[#f8f6f1]/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <Link to="/" className="text-2xl font-semibold text-emerald-900">Harvest & Co.</Link>
          <nav className="flex gap-4 text-sm">
            <Link to="/shop">Shop</Link>
            <Link to="/account/orders">Orders</Link>
            {isAdmin ? <Link to="/admin">Admin</Link> : null}
            <Link to="/cart">Cart ({qty})</Link>
            {user ? (
              <div className="flex items-center gap-2">
                {user.avatar ? (
                  <img className="h-8 w-8 rounded-full object-cover" src={user.avatar} alt="" />
                ) : (
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-800 text-xs font-medium text-white">
                    {initials(user.name || user.email)}
                  </span>
                )}
                <span className="hidden max-w-28 truncate text-emerald-900 sm:inline">{firstName(user.name || user.email)}</span>
                <button
                  type="button"
                  className="rounded-lg border border-emerald-700 px-2 py-1 text-emerald-900 hover:bg-emerald-50"
                  onClick={logout}
                >
                  Logout
                </button>
              </div>
            ) : (
              <Link to="/login" className="rounded-lg bg-emerald-700 px-3 py-1.5 text-white hover:bg-emerald-800">
                Login
              </Link>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl p-4">{children}</main>
    </div>
  )
}

export default function App() {
  const bootstrapped = useAuthStore((state) => state.bootstrapped)

  useEffect(() => {
    localStorage.setItem('guestToken', useCartStore.getState().guestToken)
    authApi.bootstrapSession()
  }, [])

  if (!bootstrapped) return <Layout><div className="p-6 text-sm text-slate-600">Restoring session...</div></Layout>

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/product/:slug" element={<ProductPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<RequireAuth><CheckoutPage /></RequireAuth>} />
        <Route path="/order/success" element={<OrderSuccess />} />
        <Route path="/account/orders" element={<RequireAuth><Orders /></RequireAuth>} />
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
  return (
    <section className="grid gap-6 py-12 md:grid-cols-2">
      <div>
        <h1 className="mb-4 text-4xl font-semibold text-emerald-900">Organic food, directly from trusted farms.</h1>
        <p className="mb-6 text-slate-600">Fresh produce, pantry staples, and dairy curated for clean living.</p>
        <Link to="/shop" className="rounded bg-emerald-700 px-4 py-2 text-white">Shop now</Link>
      </div>
      <img className="h-64 w-full rounded-xl object-cover" src="https://images.unsplash.com/photo-1542838132-92c53300491e" alt="Organic vegetables" />
    </section>
  )
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
    <div>
      {loadError ? (
        <div className="mb-4 rounded border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          {loadError}
        </div>
      ) : null}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-semibold">Shop</h2>
        <select className="rounded border p-2" value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.slug}>{c.name}</option>)}
        </select>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {products.map((p) => <ProductCard key={p.id} p={p} />)}
      </div>
    </div>
  )
}

function ProductCard({ p }: { p: Product }) {
  const { guestToken, setItems } = useCartStore()
  async function add() {
    const cart = await api.post<{ items: CartItem[] }>(`/cart?guestToken=${guestToken}`, { productId: p.id, qty: 1 })
    setItems(cart.items)
  }
  return (
    <article className="rounded-xl border border-emerald-100 bg-white p-4 shadow-sm">
      <Link to={`/product/${p.slug}`}>
        <img className="mb-3 h-40 w-full rounded object-cover" src={p.images?.[0]} alt={p.name} />
        <h3 className="font-medium">{p.name}</h3>
      </Link>
      <p className="text-sm text-slate-500">{p.unit} • {p.origin}</p>
      <p className="mt-1 font-semibold">₹{(p.pricePaise / 100).toFixed(2)}</p>
      <button onClick={add} className="mt-3 rounded bg-emerald-700 px-3 py-1.5 text-white">Add to cart</button>
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
  if (!product) return <p>Loading...</p>
  return (
    <section className="grid gap-8 md:grid-cols-2">
      <img src={product.images?.[0]} alt={product.name} className="h-96 w-full rounded object-cover" />
      <div>
        <h2 className="text-3xl font-semibold">{product.name}</h2>
        <p className="text-slate-600">{product.description}</p>
        <p className="my-3 text-xl font-semibold">₹{(product.pricePaise / 100).toFixed(2)}</p>
        <ProductCard p={product} />
      </div>
      <div className="md:col-span-2">
        <h3 className="mb-2 text-xl font-semibold">Reviews</h3>
        {reviews.length === 0 ? <p className="text-sm text-slate-500">No reviews yet.</p> : reviews.map((r) => <p key={r.id}>{r.rating}★ {r.body}</p>)}
      </div>
    </section>
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
    <div>
      <h2 className="mb-4 text-2xl font-semibold">Cart</h2>
      {items.length === 0 ? <p className="text-slate-500">Your cart is empty.</p> : null}
      <ul className="space-y-3">
        {items.map((i) => <li key={i.productId} className="rounded border bg-white p-3">{i.productId} x {i.qty}</li>)}
      </ul>
      <button disabled={!totalItems} className="mt-4 rounded bg-emerald-700 px-4 py-2 text-white disabled:opacity-40" onClick={() => navigate('/checkout')}>
        Proceed to checkout
      </button>
    </div>
  )
}

function CheckoutPage() {
  const [shippingAddress, setShippingAddress] = useState('')
  async function pay() {
    const res = await api.post<{ checkoutUrl: string }>('/checkout/sessions', { shippingAddress })
    window.location.href = res.checkoutUrl
  }

  return (
    <div className="max-w-xl">
      <h2 className="mb-4 text-2xl font-semibold">Checkout</h2>
      <textarea className="w-full rounded border p-2" rows={4} value={shippingAddress} onChange={(e) => setShippingAddress(e.target.value)} placeholder="Shipping address" />
      <button className="mt-3 rounded bg-emerald-700 px-4 py-2 text-white" onClick={pay}>Continue to payment</button>
    </div>
  )
}

function OrderSuccess() {
  return <div className="rounded bg-emerald-50 p-6 text-emerald-900">Order payment completed. Thank you for choosing Harvest & Co.</div>
}

function Orders() {
  const [orders, setOrders] = useState<any[]>([])
  useEffect(() => { api.get<any[]>('/orders').then(setOrders).catch(() => setOrders([])) }, [])
  return (
    <div>
      <h2 className="mb-4 text-2xl font-semibold">My Orders</h2>
      {orders.length === 0 ? <p className="text-slate-500">No orders yet.</p> : orders.map((o) => <div key={o.id} className="mb-2 rounded border p-3">{o.orderNumber} • {o.orderStatus}</div>)}
    </div>
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
    <div className="grid gap-4 md:grid-cols-2">
      <section className="rounded border bg-white p-4">
        <h3 className="mb-2 font-semibold">Low Stock</h3>
        {lowStock.map((s) => <p key={s.productId}>{s.productId}: {s.available}</p>)}
      </section>
      <section className="rounded border bg-white p-4">
        <h3 className="mb-2 font-semibold">Payments</h3>
        {payments.map((p) => <p key={p.id}>{p.orderNumber}: {p.status}</p>)}
      </section>
      <section className="rounded border bg-white p-4 md:col-span-2">
        <h3 className="mb-2 font-semibold">Products and Inventory</h3>
        {products.map((p) => (
          <div key={p.id} className="mb-2 flex items-center gap-2 rounded border p-2">
            <span className="min-w-60 text-sm">{p.name}</span>
            <input type="number" className="w-24 rounded border p-1" placeholder="on hand" onChange={(e) => setAdjustQty((prev) => ({ ...prev, [p.id]: Number(e.target.value) }))} />
            <button className="rounded bg-emerald-700 px-3 py-1 text-white" onClick={() => adjustStock(p.id)}>Update stock</button>
          </div>
        ))}
      </section>
      <section className="rounded border bg-white p-4 md:col-span-2">
        <h3 className="mb-2 font-semibold">Orders</h3>
        {orders.map((o) => (
          <div key={o.id} className="mb-2 flex items-center gap-2 rounded border p-2">
            <span className="min-w-56 text-sm">{o.orderNumber}</span>
            <span className="min-w-32 text-sm">{o.orderStatus}</span>
            <button className="rounded border px-2 py-1" onClick={() => updateOrderStatus(o.orderNumber, 'PACKED')}>Pack</button>
            <button className="rounded border px-2 py-1" onClick={() => updateOrderStatus(o.orderNumber, 'SHIPPED')}>Ship</button>
            <button className="rounded border px-2 py-1" onClick={() => updateOrderStatus(o.orderNumber, 'DELIVERED')}>Deliver</button>
          </div>
        ))}
      </section>
      <section className="rounded border bg-white p-4 md:col-span-2">
        <h3 className="mb-2 font-semibold">Hidden Reviews</h3>
        {hiddenReviews.length === 0 ? <p className="text-sm text-slate-500">No hidden reviews.</p> : hiddenReviews.map((r) => (
          <div key={r.id} className="mb-2 flex items-center justify-between rounded border p-2">
            <p>{r.productId}: {r.body}</p>
            <button className="rounded bg-emerald-700 px-3 py-1 text-white" onClick={() => reviewVisible(r.id)}>Make visible</button>
          </div>
        ))}
      </section>
    </div>
  )
}