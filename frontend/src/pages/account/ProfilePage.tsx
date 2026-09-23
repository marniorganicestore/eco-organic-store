import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../lib/api'
import { validateProfile } from '../../lib/profile'
import { roleLabel } from '../../lib/userDisplay'
import { useProfile, useUpdateProfile } from '../../hooks/useProfile'
import { storeBtn, storeCard, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { TextField } from '../../components/account/TextField'
import { UserAvatar } from '../../components/account/UserAvatar'

export default function ProfilePage() {
  const profile = useProfile()
  const update = useUpdateProfile()
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [avatar, setAvatar] = useState('')
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [seeded, setSeeded] = useState(false)
  const loaded = profile.data
  if (loaded && !seeded) {
    setSeeded(true)
    setName(loaded.name)
    setPhone(loaded.phone ?? '')
    setAvatar(loaded.avatar ?? '')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaved(false)
    const message = validateProfile({ name, phone, avatar })
    if (message) {
      setError(message)
      return
    }
    setError('')
    try {
      const savedProfile = await update.mutateAsync({ name: name.trim(), phone: phone.trim(), avatar: avatar.trim() })
      setName(savedProfile.name)
      setPhone(savedProfile.phone ?? '')
      setAvatar(savedProfile.avatar ?? '')
      setSaved(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to save your profile. Please try again.')
    }
  }

  return (
    <PageShell title="Profile" subtitle="The name and phone we use on your orders.">
      {profile.isPending ? <AccountSkeleton /> : null}
      {profile.isError ? (
        <FormBanner tone="error">Unable to load your profile. Refresh and try again.</FormBanner>
      ) : null}
      {profile.data ? (
        <div className="grid gap-4">
          <section className={`${storeCard} flex items-center gap-4 p-6`}>
            <UserAvatar name={profile.data.name || profile.data.email} avatar={avatar || profile.data.avatar} size="lg" />
            <div>
              <h2 className="text-xl font-semibold text-emerald-950">{profile.data.name}</h2>
              <p className="text-sm text-slate-600">{profile.data.email}</p>
              <ul className="mt-2 flex flex-wrap gap-2">
                {profile.data.roles.map((role) => (
                  <li key={role} className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-900">
                    {roleLabel(role)}
                  </li>
                ))}
                {profile.data.googleLinked ? (
                  <li className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-900">Google linked</li>
                ) : null}
              </ul>
            </div>
          </section>
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
            <TextField
              id="profile-avatar"
              label="Avatar link"
              value={avatar}
              type="url"
              inputMode="url"
              autoComplete="off"
              hint="Optional https image link. Photo upload is not available yet."
              disabled={update.isPending}
              onChange={setAvatar}
            />
            <button className={`${storeBtn} focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2`} type="submit" disabled={update.isPending} aria-busy={update.isPending}>
              {update.isPending ? 'Saving...' : 'Save profile'}
            </button>
          </form>
        </div>
      ) : null}
    </PageShell>
  )
}
