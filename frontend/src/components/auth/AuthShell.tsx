import type { ReactNode } from 'react'

type AuthShellProps = {
  title: string
  subtitle: string
  children: ReactNode
}

export function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <section className="mx-auto grid max-w-5xl overflow-hidden rounded-2xl border border-emerald-100 bg-white shadow-sm md:grid-cols-2">
      <div className="relative hidden min-h-[28rem] flex-col justify-between bg-emerald-900 p-10 text-emerald-50 md:flex">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.22em] text-emerald-200">Harvest &amp; Co.</p>
          <h1 className="mt-4 text-3xl font-semibold leading-tight">Organic food, directly from trusted farms.</h1>
          <p className="mt-3 max-w-sm text-sm leading-6 text-emerald-100/85">
            Sign in to checkout faster, follow orders, and keep your cart as you move between devices.
          </p>
        </div>
        <img
          className="mt-8 h-48 w-full rounded-xl object-cover"
          src="https://images.unsplash.com/photo-1542838132-92c53300491e"
          alt="Fresh organic vegetables"
        />
      </div>
      <div className="p-6 sm:p-10">
        <p className="mb-2 text-xs font-medium uppercase tracking-[0.18em] text-emerald-700 md:hidden">Harvest &amp; Co.</p>
        <h2 className="text-2xl font-semibold text-emerald-900">{title}</h2>
        <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
        <div className="mt-6">{children}</div>
      </div>
    </section>
  )
}
