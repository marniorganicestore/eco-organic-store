import { useState } from 'react'

type PasswordFieldProps = {
  id: string
  label: string
  value: string
  autoComplete?: string
  describedBy?: string
  invalid?: boolean
  disabled?: boolean
  onChange: (value: string) => void
}

export function PasswordField({
  id,
  label,
  value,
  autoComplete = 'current-password',
  describedBy,
  invalid,
  disabled,
  onChange
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)

  return (
    <div>
      <label className="block text-sm font-medium text-slate-800" htmlFor={id}>
        {label}
      </label>
      <div className="relative mt-1">
        <input
          id={id}
          name={id}
          className={`w-full rounded-lg border p-2.5 pr-20 outline-none focus:ring-2 focus:ring-emerald-700 ${
            invalid ? 'border-orange-300' : 'border-emerald-100'
          }`}
          type={visible ? 'text' : 'password'}
          value={value}
          autoComplete={autoComplete}
          aria-invalid={invalid || undefined}
          aria-describedby={describedBy}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value)}
        />
        <button
          type="button"
          className="absolute inset-y-0 right-1 my-1 rounded-md px-2 text-xs font-medium text-emerald-800 hover:bg-emerald-50"
          aria-pressed={visible}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? 'Hide' : 'Show'}
        </button>
      </div>
    </div>
  )
}
