import { useState } from 'react'
import { formatAddress, isDeliverableAddress } from '../lib/profile'
import { useProfile } from './useProfile'

export function useCheckoutAddress() {
  const profile = useProfile()
  const addresses = profile.data?.addresses ?? []
  const deliverable = addresses.filter(isDeliverableAddress)
  const needsDetails = addresses.some((address) => !isDeliverableAddress(address))
  const [selection, setSelection] = useState<string | null>(null)
  const [customValue, setCustomValue] = useState('')
  const preferred = deliverable.find((address) => address.defaultAddress) ?? deliverable[0]
  const resolved = profile.isSuccess || profile.isError
  const selectedId = selection ?? (resolved ? (preferred?.id ?? 'custom') : '')
  const selected = deliverable.find((address) => address.id === selectedId)

  return {
    shippingAddress: selected ? formatAddress(selected) : customValue,
    loading: profile.isPending,
    deliverable,
    needsDetails,
    selectedId,
    customValue,
    select: setSelection,
    setCustomValue
  }
}
