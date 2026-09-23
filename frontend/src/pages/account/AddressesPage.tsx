import { useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../lib/api'
import { INDIAN_STATES } from '../../lib/india'
import {
  emptyAddress,
  formatAddress,
  isDeliverableAddress,
  validateAddress,
  type Address,
  type AddressInput
} from '../../lib/profile'
import { useMakeDefaultAddress, useProfile, useRemoveAddress, useSaveAddress } from '../../hooks/useProfile'
import { harvestBtn, harvestBtnGhost, harvestCard, harvestInput, PageShell } from '../../components/layout/PageShell'
import { AccountSkeleton } from '../../components/account/AccountLayout'
import { FormBanner } from '../../components/account/FormBanner'
import { TextField } from '../../components/account/TextField'

export default function AddressesPage() {
  const profile = useProfile()
  const addresses = profile.data?.addresses ?? []
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<AddressInput>(emptyAddress)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState('')
  const [confirmRemoveId, setConfirmRemoveId] = useState<string | null>(null)
  const save = useSaveAddress(editingId ?? undefined)
  const remove = useRemoveAddress()
  const makeDefault = useMakeDefaultAddress()
  const editing = addresses.find((address) => address.id === editingId)
  const defaultLocked = addresses.length === 0 || editing?.defaultAddress === true

  function edit(address: Address) {
    setEditingId(address.id)
    setConfirmRemoveId(null)
    setError('')
    setSaved('')
    setForm({
      label: address.label,
      recipient: address.recipient,
      line1: address.line1,
      line2: address.line2,
      city: address.city,
      state: address.state,
      postalCode: address.postalCode,
      phone: address.phone,
      defaultAddress: address.defaultAddress
    })
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyAddress)
    setError('')
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaved('')
    const message = validateAddress(form)
    if (message) {
      setError(message)
      return
    }
    setError('')
    try {
      await save.mutateAsync({
        ...form,
        label: form.label.trim(),
        recipient: form.recipient.trim(),
        line1: form.line1.trim(),
        line2: form.line2.trim(),
        city: form.city.trim(),
        state: form.state,
        postalCode: form.postalCode.trim(),
        phone: form.phone.trim(),
        defaultAddress: defaultLocked || form.defaultAddress
      })
      setSaved(editingId ? 'Address updated.' : 'Address saved.')
      resetForm()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to save that address. Please try again.')
    }
  }

  async function removeAddress(id: string) {
    setError('')
    setSaved('')
    try {
      await remove.mutateAsync(id)
      if (editingId === id) resetForm()
      setConfirmRemoveId(null)
      setSaved('Address removed.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to remove that address. Please try again.')
    }
  }

  async function markDefault(id: string) {
    setError('')
    setSaved('')
    try {
      await makeDefault.mutateAsync(id)
      setSaved('Default delivery address updated.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to update the default address.')
    }
  }

  return (
    <PageShell title="Addresses" subtitle="Saved delivery spots for checkout. Orders keep a copy of the address you choose.">
      {profile.isPending ? <AccountSkeleton /> : null}
      {profile.isError ? <FormBanner tone="error">Unable to load your addresses. Refresh and try again.</FormBanner> : null}
      {profile.data ? (
        <div className="grid gap-4">
          {error ? <FormBanner tone="error">{error}</FormBanner> : null}
          {saved ? <FormBanner tone="success">{saved}</FormBanner> : null}
          {addresses.length === 0 ? (
            <section className={`${harvestCard} p-6`}>
              <h2 className="font-semibold text-emerald-950">No delivery addresses yet</h2>
              <p className="mt-1 text-sm text-slate-600">Add a home, work, or farm gate so checkout can fill itself in.</p>
            </section>
          ) : (
            <ul className="grid gap-3">
              {addresses.map((address) => (
                <li key={address.id} className={`${harvestCard} p-4`}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="font-semibold text-emerald-950">{address.label || 'Address'}</h2>
                        {address.defaultAddress ? (
                          <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-900">Default</span>
                        ) : null}
                        {isDeliverableAddress(address) ? null : (
                          <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-800">Needs details</span>
                        )}
                      </div>
                      <p className="mt-2 whitespace-pre-line text-sm text-slate-700">{formatAddress(address) || 'This saved note has no delivery details yet.'}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button type="button" className={harvestBtnGhost} onClick={() => edit(address)}>Edit</button>
                      {address.defaultAddress ? null : (
                        <button type="button" className={harvestBtnGhost} disabled={makeDefault.isPending} onClick={() => markDefault(address.id)}>
                          Set as default
                        </button>
                      )}
                      {confirmRemoveId === address.id ? (
                        <>
                          <button type="button" className={harvestBtnGhost} disabled={remove.isPending} onClick={() => removeAddress(address.id)}>
                            {remove.isPending ? 'Removing...' : 'Confirm remove'}
                          </button>
                          <button type="button" className={harvestBtnGhost} onClick={() => setConfirmRemoveId(null)}>Keep</button>
                        </>
                      ) : (
                        <button type="button" className={harvestBtnGhost} onClick={() => setConfirmRemoveId(address.id)}>Remove</button>
                      )}
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <form className={`${harvestCard} space-y-4 p-6`} onSubmit={submit} noValidate>
            <h2 className="font-semibold text-emerald-950">{editingId ? 'Edit address' : 'New address'}</h2>
            <div className="flex flex-wrap gap-2" role="group" aria-label="Address label">
              {['Home', 'Work', 'Farm'].map((label) => (
                <button
                  key={label}
                  type="button"
                  className={form.label === label ? harvestBtn : harvestBtnGhost}
                  onClick={() => setForm((current) => ({ ...current, label }))}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="address-label" label="Label" value={form.label} maxLength={40} disabled={save.isPending} onChange={(label) => setForm((current) => ({ ...current, label }))} />
              <TextField id="address-recipient" label="Recipient" value={form.recipient} autoComplete="name" disabled={save.isPending} onChange={(recipient) => setForm((current) => ({ ...current, recipient }))} />
              <TextField id="address-line1" label="Street address" value={form.line1} autoComplete="address-line1" disabled={save.isPending} onChange={(line1) => setForm((current) => ({ ...current, line1 }))} />
              <TextField id="address-line2" label="Apartment, landmark" value={form.line2} autoComplete="address-line2" hint="Optional." disabled={save.isPending} onChange={(line2) => setForm((current) => ({ ...current, line2 }))} />
              <TextField id="address-city" label="City" value={form.city} autoComplete="address-level2" disabled={save.isPending} onChange={(city) => setForm((current) => ({ ...current, city }))} />
              <label className="block text-sm font-medium text-slate-800" htmlFor="address-state">
                State or union territory
                <select
                  id="address-state"
                  className={`${harvestInput} mt-1`}
                  value={form.state}
                  disabled={save.isPending}
                  onChange={(event) => setForm((current) => ({ ...current, state: event.target.value }))}
                >
                  <option value="">Choose one</option>
                  {INDIAN_STATES.map((state) => <option key={state} value={state}>{state}</option>)}
                </select>
              </label>
              <TextField id="address-pin" label="PIN code" value={form.postalCode} inputMode="numeric" autoComplete="postal-code" maxLength={6} disabled={save.isPending} onChange={(postalCode) => setForm((current) => ({ ...current, postalCode }))} />
              <TextField id="address-phone" label="Mobile" value={form.phone} type="tel" inputMode="tel" autoComplete="tel" hint="Optional." disabled={save.isPending} onChange={(phone) => setForm((current) => ({ ...current, phone }))} />
            </div>
            <label className="flex items-center gap-2 text-sm text-slate-800" htmlFor="address-default">
              <input
                id="address-default"
                type="checkbox"
                className="h-4 w-4 accent-emerald-800"
                checked={defaultLocked || form.defaultAddress}
                disabled={defaultLocked || save.isPending}
                onChange={(event) => setForm((current) => ({ ...current, defaultAddress: event.target.checked }))}
              />
              Use as the default delivery address
            </label>
            <div className="flex flex-wrap gap-2">
              <button className={harvestBtn} type="submit" disabled={save.isPending} aria-busy={save.isPending}>
                {save.isPending ? 'Saving...' : editingId ? 'Update address' : 'Save address'}
              </button>
              {editingId ? (
                <button className={harvestBtnGhost} type="button" onClick={resetForm}>Cancel</button>
              ) : null}
            </div>
          </form>
        </div>
      ) : null}
    </PageShell>
  )
}
