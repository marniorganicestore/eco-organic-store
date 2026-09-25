import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../lib/api'
import { validateProfile } from '../../lib/profile'
import { useProfile, useUpdateProfile } from '../../hooks/useProfile'
import { storeBtn, storeCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { ProfilePhoto } from '../../components/account/ProfilePhoto'
import { TextField } from '../../components/account/TextField'
import { Link } from 'react-router-dom'

export default function ProfilePage() {
  const profile = useProfile()
  const update = useUpdateProfile()
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [seeded, setSeeded] = useState(false)
  const loaded = profile.data
  if (loaded && !seeded) {
    setSeeded(true)
    setName(loaded.name)
    setPhone(loaded.phone ?? '')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaved(false)
    const message = validateProfile({ name, phone })
    if (message) {
      setError(message)
      return
    }
    setError('')
    try {
      const savedProfile = await update.mutateAsync({ name: name.trim(), phone: phone.trim() })
      setName(savedProfile.name)
      setPhone(savedProfile.phone ?? '')
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to save your profile. Please try again.')
    }
  }

  return (
    <PageShell title="Profile" subtitle="The name, photo, and phone we use on your orders.">
      {profile.isPending ? <AccountSkeleton /> : null}
      {profile.isError ? (
        <FormBanner tone="error">Unable to load your profile. Refresh and try again.</FormBanner>
      ) : null}
      {profile.data ? (
        <div className="grid gap-4">
          <ProfilePhoto
            name={name || profile.data.name}
            email={profile.data.email}
            avatar={profile.data.avatar}
            roles={profile.data.roles}
            googleLinked={profile.data.googleLinked}
          />
          <form className={`${storeCard} space-y-4 p-6`} onSubmit={submit} noValidate>
            {error ? <FormBanner tone="error">{error}</FormBanner> : null}
            {saved ? <FormBanner tone="success">Profile saved.</FormBanner> : null}
            <TextField id="profile-name" label="Name" value={name} autoComplete="name" disabled={update.isPending} onChange={setName} />
            <p className="text-sm text-slate-600">
              Email <span className="font-medium text-emerald-950">{profile.data.email}</span> is your sign-in address.
            </p>
            <TextField
              id="profile-phone"
              label="Mobile"
              value={phone}
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              hint="Optional. Indian mobile numbers, for delivery updates."
              disabled={update.isPending}
              onChange={setPhone}
            />
            <p className="text-sm text-slate-600">
              Delivery addresses live on the{' '}
              <Link className="font-medium text-emerald-800 underline-offset-2 hover:underline" to="/account/addresses">
                Addresses
              </Link>{' '}
              page.
            </p>
            <button className={`${storeBtn} focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2`} type="submit" disabled={update.isPending} aria-busy={update.isPending}>
              {update.isPending ? 'Saving...' : 'Save profile'}
            </button>
          </form>
        </div>
      ) : null}
    </PageShell>
  )
}
