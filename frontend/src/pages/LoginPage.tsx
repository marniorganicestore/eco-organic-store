import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'
import { useAuthStore } from '../store/authStore'

function getRedirectPath(from: unknown, roles: string[]): string {
  if (typeof from === 'string' && from.startsWith('/')) return from
  return roles.includes('ADMIN') ? '/admin' : '/shop'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const bootstrapped = useAuthStore((state) => state.bootstrapped)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [idToken, setIdToken] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const from = useMemo(() => (location.state as { from?: string } | null)?.from, [location.state])

  if (bootstrapped && user) {
    return <Navigate to={getRedirectPath(from, user.roles)} replace />
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (!email.trim() || !password.trim()) {
      setError('Email and password are required.')
      return
    }
    setPending(true)
    try {
      const profile = await authApi.login({ email: email.trim(), password })
      navigate(getRedirectPath(from, profile.roles), { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to login right now.')
    } finally {
      setPending(false)
    }
  }

  async function loginWithGoogleToken() {
    setError('')
    if (!idToken.trim()) {
      setError('Google ID token is required.')
      return
    }
    setPending(true)
    try {
      const profile = await authApi.google(idToken.trim())
      navigate(getRedirectPath(from, profile.roles), { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Google login failed.')
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="mx-auto max-w-md space-y-4 rounded-xl border border-emerald-100 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold text-emerald-900">Welcome back</h2>
      <p className="text-sm text-slate-600">Login to manage orders and checkout faster.</p>
      {error ? <p className="rounded border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">{error}</p> : null}
      <form className="space-y-3" onSubmit={submit}>
        <label className="block text-sm font-medium">
          Email
          <input
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <label className="block text-sm font-medium">
          Password
          <input
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <button
          className="w-full rounded-lg bg-emerald-700 px-4 py-2 text-white disabled:opacity-50"
          disabled={pending}
          type="submit"
        >
          {pending ? 'Signing in...' : 'Login'}
        </button>
      </form>
      <div className="space-y-2 rounded border border-emerald-100 bg-emerald-50/40 p-3">
        <p className="text-xs text-slate-600">Google shell (paste GIS `idToken` from your integration)</p>
        <input
          className="w-full rounded-lg border border-emerald-100 p-2 text-sm outline-none focus:ring-2 focus:ring-emerald-700"
          value={idToken}
          onChange={(event) => setIdToken(event.target.value)}
          placeholder="Google idToken"
        />
        <button className="w-full rounded-lg border border-emerald-700 px-4 py-2 text-emerald-800" disabled={pending} onClick={loginWithGoogleToken}>
          Continue with Google token
        </button>
      </div>
      <div className="flex items-center justify-between text-sm">
        <Link className="underline" to="/forgot-password">Forgot password?</Link>
        <p>New here? <Link className="underline" to="/register">Create account</Link></p>
      </div>
    </section>
  )
}
