import { storeCard } from '../layout/PageShell'

export function AdminPending({ label }: { label: string }) {
  return (
    <div className={`${storeCard} animate-pulse space-y-3 p-6`} aria-busy="true">
      <p className="sr-only">{label}</p>
      <div className="h-5 w-40 rounded bg-emerald-100" />
      <div className="h-14 rounded-xl bg-emerald-50" />
      <div className="h-14 rounded-xl bg-emerald-50" />
    </div>
  )
}
