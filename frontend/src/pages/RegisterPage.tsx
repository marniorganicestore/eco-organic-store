import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'
import { postLoginPath } from '../lib/postLoginPath'
import { useAuthStore } from '../store/authStore'
import { AuthShell } from '../components/auth/AuthShell'
import { GoogleSignInButton } from '../components/auth/GoogleSignInButton'
import { PasswordField } from '../components/auth/PasswordField'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function RegisterPage() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const bootstrapped = useAuthStore((state) => state.bootstrapped)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)

  if (bootstrapped && user) return <Navigate to={postLoginPath(undefined, user.roles)} replace />

  async function finish(profile: { roles: string[] }) {
    navigate(postLoginPath(undefined, profile.roles), { replace: true })
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    if (name.trim().length < 2) {
      setError('Enter your full name.')
      return
    }
    if (!EMAIL_PATTERN.test(email.trim())) {
      setError('Enter a valid email address.')
      return
    }
    if (password.trim().length < 8) {
      setError('Use a password with at least 8 characters.')
      return
    }
    setPending(true)
    try {
      const profile = await authApi.register({ name: name.trim(), email: email.trim(), password })
      await finish(profile)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to create account right now.')
    } finally {
      setPending(false)
    }
  }

  async function registerWithGoogle(idToken: string) {
    setError('')
    setPending(true)
    try {
      const profile = await authApi.google(idToken)
      await finish(profile)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Google sign-in failed.')
    } finally {
      setPending(false)
    }
  }

  return (
    <AuthShell title="Create your account" subtitle="Join Harvest & Co. to save your cart and track organic orders.">
      {error ? (
        <p role="alert" className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
          {error}
        </p>
      ) : null}
      <form className="space-y-4" onSubmit={submit} noValidate>
        <label className="block text-sm font-medium text-slate-800" htmlFor="name">
          Full name
          <input
            id="name"
            name="name"
            autoComplete="name"
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700"
            value={name}
            disabled={pending}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        <label className="block text-sm font-medium text-slate-800" htmlFor="register-email">
          Email
          <input
            id="register-email"
            name="email"
            type="email"
            autoComplete="email"
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700"
            value={email}
            disabled={pending}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <PasswordField
          id="new-password"
          label="Password"
          value={password}
          autoComplete="new-password"
          disabled={pending}
          onChange={setPassword}
        />
        <p className="text-xs text-slate-500">At least 8 characters. We never store your password in plain text.</p>
        <button
          className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-50"
          disabled={pending}
          aria-busy={pending}
          type="submit"
        >
          {pending ? 'Creating account...' : 'Create account'}
        </button>
      </form>
      <GoogleSignInButton disabled={pending} onCredential={registerWithGoogle} />
      <p className="mt-6 text-sm text-slate-600">
        Already have an account?{' '}
        <Link className="font-medium text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/login">
          Sign in
        </Link>
      </p>
    </AuthShell>
  )
}
