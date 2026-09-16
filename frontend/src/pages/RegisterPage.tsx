import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'
import { useAuthStore } from '../store/authStore'

function destination(roles: string[]): string {
  return roles.includes('ADMIN') ? '/admin' : '/shop'
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const bootstrapped = useAuthStore((state) => state.bootstrapped)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  if (bootstrapped && user) return <Navigate to={destination(user.roles)} replace />

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (!name.trim() || !email.trim() || password.trim().length < 8) {
      setError('Name, valid email and min 8-char password are required.')
      return
    }
    setPending(true)
    try {
      const profile = await authApi.register({ name: name.trim(), email: email.trim(), password })
      navigate(destination(profile.roles), { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to create account right now.')
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="mx-auto max-w-md space-y-4 rounded-xl border border-emerald-100 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold text-emerald-900">Create your account</h2>
      {error ? <p className="rounded border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">{error}</p> : null}
      <form className="space-y-3" onSubmit={submit}>
        <label className="block text-sm font-medium">
          Full name
          <input className="mt-1 w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700" value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <label className="block text-sm font-medium">
          Email
          <input className="mt-1 w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
        </label>
        <label className="block text-sm font-medium">
          Password
          <input className="mt-1 w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700" type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
        </label>
        <button className="w-full rounded-lg bg-emerald-700 px-4 py-2 text-white disabled:opacity-50" disabled={pending} type="submit">
          {pending ? 'Creating...' : 'Create account'}
        </button>
      </form>
      <p className="text-sm">Already have an account? <Link className="underline" to="/login">Login</Link></p>
    </section>
  )
}
