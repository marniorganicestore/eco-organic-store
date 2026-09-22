import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'
import { AuthShell } from '../components/auth/AuthShell'
import { PasswordField } from '../components/auth/PasswordField'

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
      setError('Reset token and a password of at least 8 characters are required.')
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
    <AuthShell
      title="Reset your password"
      subtitle="We never say whether an email is registered. If an account exists, you will get the next step."
    >
      {message ? (
        <p role="status" className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900">
          {message}
        </p>
      ) : null}
      {error ? (
        <p role="alert" className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
          {error}
        </p>
      ) : null}

      <form className="space-y-3" onSubmit={requestReset}>
        <label className="block text-sm font-medium text-slate-800" htmlFor="reset-email">
          Email
          <input
            id="reset-email"
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700"
            type="email"
            autoComplete="email"
            value={email}
            disabled={pending}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <button className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-50" disabled={pending} type="submit">
          {pending ? 'Sending...' : 'Send reset request'}
        </button>
      </form>

      <form className="mt-8 space-y-3 border-t border-emerald-100 pt-6" onSubmit={confirmReset}>
        <h3 className="text-sm font-medium text-emerald-900">Have a reset token?</h3>
        <label className="block text-sm font-medium text-slate-800" htmlFor="reset-token">
          Reset token
          <input
            id="reset-token"
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700"
            value={token}
            disabled={pending}
            onChange={(event) => setToken(event.target.value)}
          />
        </label>
        <PasswordField
          id="reset-password"
          label="New password"
          value={newPassword}
          autoComplete="new-password"
          disabled={pending}
          onChange={setNewPassword}
        />
        <button className="w-full rounded-lg border border-emerald-700 px-4 py-2.5 font-medium text-emerald-800 hover:bg-emerald-50 disabled:opacity-50" disabled={pending} type="submit">
          {pending ? 'Submitting...' : 'Set new password'}
        </button>
      </form>

      <p className="mt-6 text-sm">
        <Link className="font-medium text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/login">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  )
}
