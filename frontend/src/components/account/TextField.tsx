import type { InputHTMLAttributes } from 'react'
import { harvestInput } from '../layout/PageShell'

type TextFieldProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
  autoComplete?: string
  inputMode?: InputHTMLAttributes<HTMLInputElement>['inputMode']
  maxLength?: number
  hint?: string
  disabled?: boolean
}

export function TextField({
  id,
  label,
  value,
  onChange,
  type = 'text',
  autoComplete,
  inputMode,
  maxLength,
  hint,
  disabled
}: TextFieldProps) {
  const hintId = hint ? `${id}-hint` : undefined
  return (
    <label className="block text-sm font-medium text-slate-800" htmlFor={id}>
      {label}
      <input
        id={id}
        name={id}
        className={`${harvestInput} mt-1`}
        type={type}
        value={value}
        autoComplete={autoComplete}
        inputMode={inputMode}
        maxLength={maxLength}
        aria-describedby={hintId}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
      />
      {hint ? <span id={hintId} className="mt-1 block text-xs font-normal text-slate-500">{hint}</span> : null}
    </label>
  )
}
