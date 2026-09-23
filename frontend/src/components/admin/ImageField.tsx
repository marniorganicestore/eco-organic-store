import { useState } from 'react'
import type { DragEvent } from 'react'
import { ApiError, api } from '../../lib/api'
import { fileIssue, imageAddressIssue, resolveImageSrc, type MediaUpload } from '../../lib/media'
import { FormBanner } from '../account/FormBanner'
import { storeBtnGhost, storeInput } from '../layout/PageShell'

type ImageFieldProps = {
  id: string
  label: string
  images: string[]
  max?: number
  disabled?: boolean
  onChange: (images: string[]) => void
}

export function ImageField({ id, label, images, max = 1, disabled = false, onChange }: ImageFieldProps) {
  const [address, setAddress] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [dragging, setDragging] = useState(false)
  const room = Math.max(0, max - images.length)
  const canAdd = room > 0 && !disabled && !busy

  async function upload(files: File[]) {
    const fitting = max === 1 && images.length > 0 ? files.slice(0, 1) : files.slice(0, room)
    if (fitting.length === 0) {
      setError(max === 1 ? 'Replace the current photo, or remove it first.' : `This product already has ${max} photos.`)
      return
    }
    for (const file of fitting) {
      const message = fileIssue(file)
      if (message) {
        setError(message)
        return
      }
    }
    setError('')
    setBusy(true)
    const next = max === 1 ? [] : [...images]
    try {
      for (const file of fitting) {
        const saved = await api.upload<MediaUpload>('/admin/catalog/images', file)
        next.push(saved.url)
        onChange([...next])
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to upload that photo.')
    } finally {
      setBusy(false)
    }
  }

  function addAddress() {
    const message = imageAddressIssue(address)
    if (message) {
      setError(message)
      return
    }
    const trimmed = address.trim()
    if (images.includes(trimmed)) {
      setAddress('')
      return
    }
    if (max === 1) {
      onChange([trimmed])
    } else if (room === 0) {
      setError(`This product already has ${max} photos.`)
      return
    } else {
      onChange([...images, trimmed])
    }
    setAddress('')
    setError('')
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragging(false)
    if (!canAdd && !(max === 1 && !disabled && !busy)) return
    const files = Array.from(event.dataTransfer.files)
    if (files.length > 0) void upload(files)
  }

  return (
    <fieldset className="min-w-0 border-0 p-0" aria-busy={busy}>
      <legend className="text-sm font-medium text-slate-800">{label}</legend>
      <p className="mt-1 text-xs text-slate-500">
        JPEG, PNG, or WebP, up to 1.5 MB. Paste an address if the photo is already online.
        {max > 1 ? ' The first photo is the shop cover.' : ''}
      </p>
      {error ? <div className="mt-3"><FormBanner tone="error">{error}</FormBanner></div> : null}
      {images.length > 0 ? (
        <ul className={`mt-3 grid gap-3 ${max > 1 ? 'sm:grid-cols-3' : 'sm:grid-cols-2'}`}>
          {images.map((src, index) => (
            <li key={`${src}-${index}`} className="overflow-hidden rounded-xl border border-emerald-100 bg-white/80">
              <img
                src={resolveImageSrc(src)}
                alt={index === 0 && max > 1 ? 'Cover photo preview' : 'Photo preview'}
                className="h-36 w-full object-cover"
              />
              <div className="flex items-center justify-between gap-2 px-2 py-2">
                {max > 1 && index === 0 ? <span className="text-xs font-medium text-emerald-800">Cover</span> : <span />}
                <div className="flex gap-2">
                  {max > 1 && index > 0 ? (
                    <button
                      type="button"
                      className={storeBtnGhost}
                      disabled={disabled || busy}
                      onClick={() => {
                        const next = [...images]
                        const [chosen] = next.splice(index, 1)
                        onChange([chosen, ...next])
                      }}
                    >
                      Make cover
                    </button>
                  ) : null}
                  <button
                    type="button"
                    className={storeBtnGhost}
                    disabled={disabled || busy}
                    onClick={() => onChange(images.filter((_, imageIndex) => imageIndex !== index))}
                  >
                    Remove
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      ) : null}
      {canAdd || (max === 1 && !disabled) ? (
        <label
          htmlFor={id}
          onDragOver={(event) => {
            event.preventDefault()
            if (!disabled && !busy) setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
          className={`mt-3 flex cursor-pointer flex-col items-center rounded-xl border border-dashed px-4 py-6 text-center ${
            dragging ? 'border-emerald-700 bg-emerald-50' : 'border-emerald-300 bg-white/70'
          } ${disabled || busy ? 'cursor-not-allowed opacity-60' : ''}`}
        >
          <input
            id={id}
            type="file"
            accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
            multiple={max > 1}
            className="sr-only"
            disabled={disabled || busy}
            onChange={(event) => {
              const files = event.target.files ? Array.from(event.target.files) : []
              event.target.value = ''
              if (files.length > 0) void upload(files)
            }}
          />
          <span className="text-sm font-medium text-emerald-950">
            {busy ? 'Uploading...' : images.length > 0 && max === 1 ? 'Drop a new photo to replace it, or browse' : 'Drop a photo here, or browse'}
          </span>
          <span className="mt-1 text-xs text-slate-500">
            {max > 1 ? `${images.length} of ${max} photos` : 'Shown on the shop card'}
          </span>
        </label>
      ) : null}
      <div className="mt-3 flex flex-col gap-2 sm:flex-row">
        <label className="min-w-0 flex-1 text-sm font-medium text-slate-800" htmlFor={`${id}-address`}>
          Image address
          <input
            id={`${id}-address`}
            className={`${storeInput} mt-1`}
            value={address}
            placeholder="https://"
            disabled={disabled || busy}
            onChange={(event) => setAddress(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault()
                addAddress()
              }
            }}
          />
        </label>
        <button
          type="button"
          className={`${storeBtnGhost} sm:mt-6`}
          disabled={disabled || busy}
          onClick={addAddress}
        >
          Use address
        </button>
      </div>
    </fieldset>
  )
}
