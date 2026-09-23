import { Link } from 'react-router-dom'
import { formatAddress, type Address } from '../../lib/profile'
import { harvestCard, harvestInput } from '../layout/PageShell'

type ShippingAddressPickerProps = {
  loading: boolean
  deliverable: Address[]
  needsDetails: boolean
  selectedId: string
  customValue: string
  onSelect: (id: string) => void
  onCustomChange: (value: string) => void
}

export function ShippingAddressPicker({
  loading,
  deliverable,
  needsDetails,
  selectedId,
  customValue,
  onSelect,
  onCustomChange
}: ShippingAddressPickerProps) {
  const showCustom = selectedId === 'custom' || (!loading && deliverable.length === 0 && selectedId !== '')

  return (
    <div className="space-y-4">
      {loading ? <p className="text-sm text-slate-500">Looking up saved addresses...</p> : null}
      {deliverable.length > 0 ? (
        <fieldset className="space-y-2">
          <legend className="text-sm font-medium text-slate-800">Saved addresses</legend>
          {deliverable.map((address) => (
            <label key={address.id} className={`${harvestCard} flex cursor-pointer gap-3 p-3 text-sm`}>
              <input
                type="radio"
                name="saved-address"
                className="mt-1 accent-emerald-800"
                checked={selectedId === address.id}
                onChange={() => onSelect(address.id)}
              />
              <span>
                <span className="font-medium text-emerald-950">
                  {address.label || 'Address'}
                  {address.defaultAddress ? ' · Default' : ''}
                </span>
                <span className="mt-1 block whitespace-pre-line text-slate-600">{formatAddress(address)}</span>
              </span>
            </label>
          ))}
          <label className={`${harvestCard} flex cursor-pointer gap-3 p-3 text-sm`}>
            <input
              type="radio"
              name="saved-address"
              className="mt-1 accent-emerald-800"
              checked={selectedId === 'custom'}
              onChange={() => onSelect('custom')}
            />
            <span className="font-medium text-emerald-950">Use a different address</span>
          </label>
        </fieldset>
      ) : null}
      {needsDetails ? (
        <p className="text-sm text-slate-600">
          A saved address is missing details.{' '}
          <Link className="font-medium text-emerald-800 underline decoration-emerald-300 underline-offset-2" to="/account/addresses">
            Finish it in your account
          </Link>
        </p>
      ) : null}
      {showCustom ? (
        <label className="block text-sm font-medium text-slate-800" htmlFor="shipping">
          Shipping address
          <textarea
            id="shipping"
            className={`${harvestInput} mt-1`}
            rows={4}
            value={customValue}
            autoComplete="street-address"
            placeholder="Recipient, street, city, state, PIN"
            onChange={(event) => onCustomChange(event.target.value)}
          />
        </label>
      ) : null}
    </div>
  )
}
