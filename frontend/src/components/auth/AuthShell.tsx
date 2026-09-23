import type { ReactNode } from 'react'

type AuthShellProps = {
  title: string
  subtitle: string
  children: ReactNode
  wide?: boolean
  asideTitle?: string
  asideBody?: string
}

const AUTH_SCENE_SRC = '/images/login-scene.png'

export function AuthShell({
  title,
  subtitle,
  children,
  wide = false,
  asideTitle = 'Organic food, directly from trusted farms.',
  asideBody = 'Sign in to checkout faster, follow orders, and keep your cart as you move between devices.'
}: AuthShellProps) {
  return (
    <div className="relative isolate min-h-[calc(100vh-4.75rem)] overflow-hidden">
      <img
        src={AUTH_SCENE_SRC}
        alt=""
        width={1920}
        height={1080}
        decoding="async"
        fetchPriority="high"
        aria-hidden="true"
        className="absolute inset-0 h-full w-full scale-105 object-cover object-center"
      />
      <div className="absolute inset-0 bg-linear-to-br from-emerald-950/80 via-emerald-950/45 to-amber-950/40" />
      <div className="absolute inset-x-0 bottom-0 h-40 bg-linear-to-t from-emerald-950/70 to-transparent" />
      <div className={`relative mx-auto grid min-h-[calc(100vh-4.75rem)] max-w-6xl items-center gap-10 px-4 py-10 ${
        wide ? 'lg:grid-cols-[1fr_minmax(22rem,34rem)]' : 'lg:grid-cols-[1fr_26rem]'
      }`}>
        <div className="hidden max-w-lg text-white lg:block">
          <p className="text-xs font-medium uppercase tracking-[0.28em] text-emerald-100/90">Eco Organic Store</p>
          <h1 className="mt-4 text-4xl font-semibold leading-tight text-white drop-shadow-sm">
            {asideTitle}
          </h1>
          <p className="mt-4 max-w-md text-base leading-7 text-emerald-50/90">
            {asideBody}
          </p>
        </div>
        <div className={`mx-auto w-full rounded-2xl border border-white/50 bg-[#f8f6f1]/95 p-6 shadow-[0_24px_80px_rgba(6,46,28,0.35)] backdrop-blur-md sm:p-8 ${
          wide ? 'max-w-lg' : 'max-w-md'
        }`}>
          <p className="mb-2 text-xs font-medium uppercase tracking-[0.18em] text-emerald-700 lg:hidden">Eco Organic Store</p>
          <h2 className="text-2xl font-semibold text-emerald-900">{title}</h2>
          <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  )
}
