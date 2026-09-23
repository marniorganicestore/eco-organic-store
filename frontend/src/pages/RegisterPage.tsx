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

type Field = 'name' | 'email' | 'password' | 'confirm' | ''

function registerErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401 && /disabled/i.test(error.message)) {
    return 'This account is disabled. Contact the store.'
  }
  if (error instanceof ApiError) return error.message
  return 'Unable to create account right now.'
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const bootstrapped = useAuthStore((state) => state.bootstrapped)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState<Field>('')
  const [pending, setPending] = useState(false)

  if (bootstrapped && user) return <Navigate to={postLoginPath(undefined, user.roles)} replace />

  async function finish(profile: { roles: string[] }) {
    navigate(postLoginPath(undefined, profile.roles), { replace: true })
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFieldError('')
    if (name.trim().length < 2) {
      setFieldError('name')
      setError('Enter your full name.')
      return
    }
    const trimmedEmail = email.trim()
    if (!EMAIL_PATTERN.test(trimmedEmail)) {
      setFieldError('email')
      setError('Enter a valid email address.')
      return
    }
    if (password.length < 8 || password.length > 72) {
      setFieldError('password')
      setError('Use a password with 8–72 characters.')
      return
    }
    if (password !== confirm) {
      setFieldError('confirm')
      setError('Passwords do not match.')
      return
    }
    setPending(true)
    try {
      const profile = await authApi.register({ name: name.trim(), email: trimmedEmail, password })
      await finish(profile)
    } catch (err) {
      setError(registerErrorMessage(err))
    } finally {
      setPending(false)
    }
  }

  async function registerWithGoogle(idToken: string) {
    setError('')
    setFieldError('')
    setPending(true)
    try {
      const profile = await authApi.google(idToken)
      await finish(profile)
    } catch (err) {
      setError(registerErrorMessage(err))
    } finally {
      setPending(false)
    }
  }

  const inputClass = (field: Field) =>
    `mt-1 w-full rounded-lg border p-2.5 outline-none focus:ring-2 focus:ring-emerald-700 ${
      fieldError === field ? 'border-orange-300' : 'border-emerald-100'
    }`

  return (
    <AuthShell
      title="Create your account"
      subtitle="You will shop as a customer. Admin access is not chosen here."
      asideTitle="Join Eco Organic Store as a customer."
      asideBody="Save your cart and follow organic orders. If you also run the store, an admin grants that access after you sign up."
    >
      {error ? (
        <p id="register-error" role="alert" className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
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
            autoFocus
            className={inputClass('name')}
            value={name}
            aria-invalid={fieldError === 'name' || undefined}
            aria-describedby={error ? 'register-error' : undefined}
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
            inputMode="email"
            autoComplete="email"
            className={inputClass('email')}
            value={email}
            aria-invalid={fieldError === 'email' || undefined}
            aria-describedby={error ? 'register-error' : undefined}
            disabled={pending}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <PasswordField
          id="new-password"
          label="Password"
          value={password}
          autoComplete="new-password"
          invalid={fieldError === 'password'}
          describedBy={error ? 'register-error' : 'password-hint'}
          disabled={pending}
          onChange={setPassword}
        />
        <p id="password-hint" className="text-xs text-slate-500">At least 8 characters. We store only a secure hash, never the password itself.</p>
        <PasswordField
          id="confirm-password"
          label="Confirm password"
          value={confirm}
          autoComplete="new-password"
          invalid={fieldError === 'confirm'}
          describedBy={error ? 'register-error' : undefined}
          disabled={pending}
          onChange={setConfirm}
        />
        <button
          className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white outline-none hover:bg-emerald-800 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2 disabled:opacity-50"
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
