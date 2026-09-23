import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, authApi } from '../../lib/api'
import { validatePasswordChange } from '../../lib/profile'
import { useProfile } from '../../hooks/useProfile'
import { storeBtn, storeCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { PasswordField } from '../../components/auth/PasswordField'

export default function SecurityPage() {
  const profile = useProfile()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [pending, setPending] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaved(false)
    const message = validatePasswordChange(currentPassword, newPassword, confirmPassword)
    if (message) {
      setError(message)
      return
    }
    setError('')
    setPending(true)
    try {
      await authApi.changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to change your password. Please try again.')
    } finally {
      setPending(false)
    }
  }

  return (
    <PageShell title="Security" subtitle="Password for email sign-in. Google stays linked if you added it.">
      {profile.isPending ? <AccountSkeleton /> : null}
      {profile.isError ? <FormBanner tone="error">Unable to load your account. Refresh and try again.</FormBanner> : null}
      {profile.data && !profile.data.passwordSet ? (
        <section className={`${storeCard} p-6`}>
          <h2 className="font-semibold text-emerald-950">Google sign-in</h2>
          <p className="mt-2 text-sm text-slate-600">
            {profile.data.email} signs in with Google, so there is no password to change.
          </p>
        </section>
      ) : null}
      {profile.data?.passwordSet ? (
        <form className={`${storeCard} max-w-lg space-y-4 p-6`} onSubmit={submit} noValidate>
          {error ? <FormBanner tone="error">{error}</FormBanner> : null}
          {saved ? <FormBanner tone="success">Password updated. Other sessions have been signed out.</FormBanner> : null}
          <PasswordField id="current-password" label="Current password" value={currentPassword} autoComplete="current-password" disabled={pending} onChange={setCurrentPassword} />
          <PasswordField id="new-password" label="New password" value={newPassword} autoComplete="new-password" disabled={pending} onChange={setNewPassword} />
          <PasswordField id="confirm-password" label="Confirm new password" value={confirmPassword} autoComplete="new-password" disabled={pending} onChange={setConfirmPassword} />
          <p className="text-xs text-slate-500">Use 8–72 characters. This signs out your other devices.</p>
          <button className={storeBtn} type="submit" disabled={pending} aria-busy={pending}>
            {pending ? 'Updating...' : 'Update password'}
          </button>
        </form>
      ) : null}
    </PageShell>
  )
}
