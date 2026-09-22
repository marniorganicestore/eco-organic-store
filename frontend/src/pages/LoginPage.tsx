import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError, authApi } from '../lib/api'
import { postLoginPath } from '../lib/postLoginPath'
import { useAuthStore } from '../store/authStore'
import { AuthShell } from '../components/auth/AuthShell'
import { GoogleSignInButton } from '../components/auth/GoogleSignInButton'
import { PasswordField } from '../components/auth/PasswordField'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function loginErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Invalid email or password.'
    if (error.status === 400) return 'Check your email and password and try again.'
    return error.message
  }
  return 'Unable to sign in right now. Please try again.'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const bootstrapped = useAuthStore((state) => state.bootstrapped)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState<'email' | 'password' | ''>('')
  const [pending, setPending] = useState(false)
  const from = useMemo(() => (location.state as { from?: string } | null)?.from, [location.state])

  if (bootstrapped && user) {
    return <Navigate to={postLoginPath(from, user.roles)} replace />
  }

  async function finishLogin(profile: { roles: string[] }) {
    navigate(postLoginPath(from, profile.roles), { replace: true })
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFieldError('')
    const trimmedEmail = email.trim()
    if (!EMAIL_PATTERN.test(trimmedEmail)) {
      setFieldError('email')
      setError('Enter a valid email address.')
      return
    }
    if (!password) {
      setFieldError('password')
      setError('Enter your password.')
      return
    }
    setPending(true)
    try {
      const profile = await authApi.login({ email: trimmedEmail, password })
      await finishLogin(profile)
    } catch (err) {
      setError(loginErrorMessage(err))
    } finally {
      setPending(false)
    }
  }

  async function loginWithGoogle(idToken: string) {
    setError('')
    setFieldError('')
    setPending(true)
    try {
      const profile = await authApi.google(idToken)
      await finishLogin(profile)
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401
        ? 'Google sign-in failed. Try again or use your email and password.'
        : loginErrorMessage(err))
    } finally {
      setPending(false)
    }
  }

  return (
    <AuthShell scene title="Welcome back" subtitle="Login to manage orders and checkout faster.">
      {error ? (
        <p id="login-error" role="alert" className="mb-4 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
          {error}
        </p>
      ) : null}
      <form className="space-y-4" onSubmit={submit} noValidate>
        <label className="block text-sm font-medium text-slate-800" htmlFor="email">
          Email
          <input
            id="email"
            name="email"
            className="mt-1 w-full rounded-lg border border-emerald-100 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700"
            type="email"
            inputMode="email"
            autoComplete="username"
            autoFocus
            value={email}
            aria-invalid={fieldError === 'email' || undefined}
            aria-describedby={error ? 'login-error' : undefined}
            disabled={pending}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <PasswordField
          id="password"
          label="Password"
          value={password}
          invalid={fieldError === 'password'}
          describedBy={error ? 'login-error' : undefined}
          disabled={pending}
          onChange={setPassword}
        />
        <div className="flex justify-end">
          <Link className="text-sm text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/forgot-password">
            Forgot password?
          </Link>
        </div>
        <button
          className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white hover:bg-emerald-800 disabled:opacity-50"
          disabled={pending}
          aria-busy={pending}
          type="submit"
        >
          {pending ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
      <GoogleSignInButton disabled={pending} onCredential={loginWithGoogle} />
      <p className="mt-6 text-sm text-slate-600">
        New here?{' '}
        <Link className="font-medium text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/register">
          Create an account
        </Link>
      </p>
    </AuthShell>
  )
}
