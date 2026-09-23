type QtyStepperProps = {
  qty: number
  min?: number
  max?: number
  label: string
  disabled?: boolean
  onChange: (qty: number) => void
}

export function QtyStepper({ qty, min = 1, max = 24, label, disabled = false, onChange }: QtyStepperProps) {
  return (
    <div className="inline-flex items-center rounded-lg border border-emerald-200 bg-white">
      <button
        type="button"
        className="px-3 py-1.5 text-lg leading-none text-emerald-900 outline-none hover:bg-emerald-50 focus-visible:ring-2 focus-visible:ring-emerald-700 disabled:opacity-40"
        aria-label={`Decrease quantity of ${label}`}
        disabled={disabled || qty <= min}
        onClick={() => onChange(qty - 1)}
      >
        −
      </button>
      <span className="min-w-8 text-center text-sm font-medium tabular-nums text-emerald-950">{qty}</span>
      <button
        type="button"
        className="px-3 py-1.5 text-lg leading-none text-emerald-900 outline-none hover:bg-emerald-50 focus-visible:ring-2 focus-visible:ring-emerald-700 disabled:opacity-40"
        aria-label={`Increase quantity of ${label}`}
        disabled={disabled || qty >= max}
        onClick={() => onChange(qty + 1)}
      >
        +
      </button>
    </div>
  )
}
