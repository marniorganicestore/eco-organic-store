import { statusLabel } from '../../lib/adminDesk'

const tones: Record<string, string> = {
  CONFIRMED: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  PACKED: 'border-emerald-200 bg-white text-emerald-950',
  SHIPPED: 'border-emerald-300 bg-emerald-100 text-emerald-950',
  DELIVERED: 'border-emerald-800/20 bg-emerald-800 text-white',
  PENDING_PAYMENT: 'border-amber-200 bg-amber-50 text-amber-950',
  PENDING: 'border-amber-200 bg-amber-50 text-amber-950',
  PAID: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  CANCELLED: 'border-orange-200 bg-orange-50 text-orange-900',
  VISIBLE: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  HIDDEN: 'border-orange-200 bg-orange-50 text-orange-900',
  LOW: 'border-orange-200 bg-orange-50 text-orange-900',
  Active: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  Hidden: 'border-slate-200 bg-slate-50 text-slate-700'
}

export function StatusPill({ status, label }: { status: string; label?: string }) {
  const tone = tones[status] ?? 'border-emerald-100 bg-white text-emerald-950'
  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium ${tone}`}>
      {label ?? statusLabel(status)}
    </span>
  )
}
