import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  async function requestReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    if (!email.trim()) {
      setError('Email is required.')
      return
    }
    setPending(true)
    try {
      const response = await authApi.requestReset(email.trim())
      setMessage(response.message)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to submit request.')
    } finally {
      setPending(false)
    }
  }

  async function confirmReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    if (!token.trim() || newPassword.trim().length < 8) {
      setError('Reset token and min 8-char password are required.')
      return
    }
    setPending(true)
    try {
      const response = await authApi.confirmReset(token.trim(), newPassword)
      setMessage(response.message)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to confirm reset.')
    } finally {
      setPending(false)
    }
  }

  return (
    <section className="mx-auto max-w-xl space-y-4 rounded-xl border border-emerald-100 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold text-emerald-900">Reset password</h2>
      <p className="text-sm text-slate-600">This flow is intentionally generic and does not reveal whether an account exists.</p>
      {message ? <p className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900">{message}</p> : null}
      {error ? <p className="rounded border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">{error}</p> : null}

      <form className="space-y-3 rounded border border-emerald-100 p-4" onSubmit={requestReset}>
        <h3 className="font-medium">Request reset</h3>
        <input className="w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="Email" />
        <button className="rounded-lg bg-emerald-700 px-4 py-2 text-white disabled:opacity-50" disabled={pending} type="submit">
          {pending ? 'Sending...' : 'Send reset request'}
        </button>
      </form>

      <form className="space-y-3 rounded border border-emerald-100 p-4" onSubmit={confirmReset}>
        <h3 className="font-medium">Confirm reset (shell)</h3>
        <input className="w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700" value={token} onChange={(event) => setToken(event.target.value)} placeholder="Reset token" />
        <input className="w-full rounded-lg border border-emerald-100 p-2 outline-none focus:ring-2 focus:ring-emerald-700" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} placeholder="New password (min 8 chars)" />
        <button className="rounded-lg border border-emerald-700 px-4 py-2 text-emerald-800 disabled:opacity-50" disabled={pending} type="submit">
          {pending ? 'Submitting...' : 'Confirm reset'}
        </button>
      </form>

      <p className="text-sm"><Link className="underline" to="/login">Back to login</Link></p>
    </section>
  )
}
