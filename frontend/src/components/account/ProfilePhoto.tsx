import { useRef, useState } from 'react'
import type { DragEvent, FormEvent } from 'react'
import { ApiError } from '../../lib/api'
import { fileIssue } from '../../lib/media'
import { validateProfile } from '../../lib/profile'
import { roleLabel } from '../../lib/userDisplay'
import { useAvatarLink, useRemoveAvatar, useUploadAvatar } from '../../hooks/useProfile'
import { storeBtnGhost, storeCard, storeInput } from '../layout/PageShell'
import { FormBanner } from './FormBanner'
import { UserAvatar } from './UserAvatar'

type ProfilePhotoProps = {
  name: string
  email: string
  avatar: string | null
  roles: string[]
  googleLinked: boolean
}

export function ProfilePhoto({ name, email, avatar, roles, googleLinked }: ProfilePhotoProps) {
  const upload = useUploadAvatar()
  const remove = useRemoveAvatar()
  const link = useAvatarLink()
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [dragging, setDragging] = useState(false)
  const [showLink, setShowLink] = useState(false)
  const [address, setAddress] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)
  const busy = upload.isPending || remove.isPending || link.isPending
  const label = name || email

  async function send(file: File) {
    const message = fileIssue(file)
    if (message) {
      setNotice('')
      setError(message)
      return
    }
    setError('')
    setNotice('')
    try {
      await upload.mutateAsync(file)
      setAddress('')
      setNotice('Photo updated.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to upload that photo.')
    }
  }

  async function clear() {
    setError('')
    setNotice('')
    try {
      await remove.mutateAsync()
      setAddress('')
      setNotice('Photo removed.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to remove your photo.')
    }
  }

  async function applyLink(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = validateProfile({ name: label.length >= 2 ? label : 'Member', phone: '', avatar: address })
    if (message) {
      setNotice('')
      setError(message)
      return
    }
    setError('')
    setNotice('')
    try {
      await link.mutateAsync(address.trim())
      setNotice(address.trim() ? 'Photo link saved.' : 'Photo removed.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to save that photo link.')
    }
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragging(false)
    if (busy) return
    const file = event.dataTransfer.files[0]
    if (file) void send(file)
  }

  return (
    <section className={`${storeCard} p-6`} aria-busy={busy}>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
        <label
          htmlFor="profile-photo"
          onDragOver={(event) => {
            event.preventDefault()
            if (!busy) setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          className={`group relative mx-auto shrink-0 cursor-pointer rounded-full outline-none focus-within:ring-2 focus-within:ring-emerald-700 focus-within:ring-offset-2 sm:mx-0 ${
            dragging ? 'ring-2 ring-emerald-700 ring-offset-2' : ''
          } ${busy ? 'cursor-wait opacity-70' : ''}`}
        >
          <UserAvatar name={label} avatar={avatar} size="xl" alt="" />
          <input
            ref={fileRef}
            id="profile-photo"
            type="file"
            accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
            aria-label="Upload profile photo"
            className="sr-only"
            disabled={busy}
            onChange={(event) => {
              const file = event.target.files?.[0]
              event.target.value = ''
              if (file) void send(file)
            }}
          />
        </label>
        <div className="min-w-0 flex-1 text-center sm:text-left">
          <h2 className="text-xl font-semibold text-emerald-950">{label}</h2>
          <p className="text-sm text-slate-600">{email}</p>
          <ul className="mt-2 flex flex-wrap justify-center gap-2 sm:justify-start">
            {roles.map((role) => (
              <li key={role} className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-900">
                {roleLabel(role)}
              </li>
            ))}
            {googleLinked ? (
              <li className="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-900">Google linked</li>
            ) : null}
          </ul>
          <p className="mt-3 text-xs text-slate-500">JPEG, PNG, or WebP, up to 1.5 MB. Shown beside your name in the header.</p>
          <div className="mt-3 flex flex-wrap justify-center gap-2 sm:justify-start">
            <button
              type="button"
              className={storeBtnGhost}
              disabled={busy}
              onClick={() => fileRef.current?.click()}
            >
              {upload.isPending ? 'Uploading...' : avatar ? 'Change photo' : 'Upload photo'}
            </button>
            {avatar ? (
              <button type="button" className={storeBtnGhost} disabled={busy} onClick={() => void clear()}>
                {remove.isPending ? 'Removing...' : 'Remove'}
              </button>
            ) : null}
            <button
              type="button"
              className={storeBtnGhost}
              disabled={busy}
              aria-expanded={showLink}
              onClick={() => setShowLink((open) => !open)}
            >
              Use a link
            </button>
          </div>
        </div>
      </div>
      {error ? <div className="mt-3"><FormBanner tone="error">{error}</FormBanner></div> : null}
      {notice ? <div className="mt-3"><FormBanner tone="success">{notice}</FormBanner></div> : null}
      {showLink ? (
        <form className="mt-3 flex flex-col gap-2 sm:flex-row" onSubmit={applyLink}>
          <label className="min-w-0 flex-1 text-sm font-medium text-slate-800" htmlFor="profile-avatar-link">
            Image link
            <input
              id="profile-avatar-link"
              className={`${storeInput} mt-1`}
              type="url"
              inputMode="url"
              autoComplete="off"
              placeholder="https://"
              value={address}
              disabled={busy}
              onChange={(event) => setAddress(event.target.value)}
            />
          </label>
          <button type="submit" className={`${storeBtnGhost} sm:mt-6`} disabled={busy}>
            {link.isPending ? 'Saving...' : 'Save link'}
          </button>
        </form>
      ) : null}
    </section>
  )
}
