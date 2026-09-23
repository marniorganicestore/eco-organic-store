import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'
import { STORE_MAILBOX } from '../lib/mail'
import { AuthShell } from '../components/auth/AuthShell'
import { PasswordField } from '../components/auth/PasswordField'

export default function ForgotPasswordPage() {
  const [params] = useSearchParams()
  const tokenFromLink = params.get('token')?.trim() ?? ''
  const [email, setEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const choosingPassword = tokenFromLink.length > 0

  async function requestReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    if (!email.trim().includes('@')) {
      setError('Enter a valid email address.')
      return
    }
    setPending(true)
    try {
      const response = await authApi.requestReset(email.trim())
      setMessage(response.message)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to send the reset email.')
    } finally {
      setPending(false)
    }
  }

  async function confirmReset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setError('')
    if (newPassword.trim().length < 8) {
      setError('Use a password with at least 8 characters.')
      return
    }
    setPending(true)
    try {
      const response = await authApi.confirmReset(tokenFromLink, newPassword)
      setMessage(response.message)
      setNewPassword('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to update the password.')
    } finally {
      setPending(false)
    }
  }

  return (
    <AuthShell
      title={choosingPassword ? 'Choose a new password' : 'Reset your password'}
      subtitle={
        choosingPassword
          ? 'This signs out other devices. Then sign in with the new password.'
          : `If an account exists, a message from ${STORE_MAILBOX} arrives within a few minutes. The link expires in 30 minutes.`
      }
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

      {choosingPassword ? (
        <form className="space-y-3" onSubmit={confirmReset}>
          <PasswordField
            id="reset-password"
            label="New password"
            value={newPassword}
            autoComplete="new-password"
            disabled={pending}
            onChange={setNewPassword}
          />
          <p className="text-xs text-slate-500">At least 8 characters. We store only a secure hash.</p>
          <button className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-50" disabled={pending} type="submit">
            {pending ? 'Updating...' : 'Set new password'}
          </button>
        </form>
      ) : (
        <form className="space-y-3" onSubmit={requestReset}>
          <label className="block text-sm font-medium text-slate-800" htmlFor="reset-email">
            Email
            <input
              id="reset-email"
              className="mt-1 w-full rounded-lg border border-emerald-100 bg-white/85 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700"
              type="email"
              autoComplete="email"
              value={email}
              disabled={pending}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <button className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-50" disabled={pending} type="submit">
            {pending ? 'Sending...' : 'Email me a reset link'}
          </button>
        </form>
      )}

      <p className="mt-6 text-sm">
        <Link className="font-medium text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/login">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  )
}
