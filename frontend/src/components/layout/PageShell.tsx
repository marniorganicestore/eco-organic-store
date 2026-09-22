import type { ReactNode } from 'react'

type PageShellProps = {
  title: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
}

export const harvestCard =
  'rounded-2xl border border-white/70 bg-[#f8f6f1]/95 shadow-[0_18px_50px_rgba(6,46,28,0.12)] backdrop-blur-md'
export const harvestBtn =
  'rounded-lg bg-emerald-700 px-4 py-2.5 font-medium text-white shadow-sm hover:bg-emerald-800 disabled:opacity-40'
export const harvestBtnGhost =
  'rounded-lg border border-emerald-700/70 px-3 py-1.5 text-sm font-medium text-emerald-900 hover:bg-emerald-50'
export const harvestInput =
  'w-full rounded-lg border border-emerald-100 bg-white/85 p-2.5 outline-none focus:ring-2 focus:ring-emerald-700'

export function PageShell({ title, subtitle, actions, children }: PageShellProps) {
  return (
    <div>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.22em] text-emerald-800/80">Harvest &amp; Co.</p>
          <h1 className="mt-1 text-3xl font-semibold text-emerald-950">{title}</h1>
          {subtitle ? <p className="mt-1 max-w-xl text-sm text-slate-600">{subtitle}</p> : null}
        </div>
        {actions}
      </div>
      {children}
    </div>
  )
}
