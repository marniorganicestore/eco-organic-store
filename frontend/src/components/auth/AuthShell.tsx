import type { ReactNode } from 'react'

type AuthShellProps = {
  title: string
  subtitle: string
  children: ReactNode
  scene?: boolean
}

const LOGIN_SCENE_SRC = '/images/login-harvest.png'

export function AuthShell({ title, subtitle, children, scene = false }: AuthShellProps) {
  if (scene) {
    return (
      <div className="relative isolate min-h-[calc(100vh-4.75rem)] overflow-hidden">
        <img
          src={LOGIN_SCENE_SRC}
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
        <div className="relative mx-auto grid min-h-[calc(100vh-4.75rem)] max-w-6xl items-center gap-10 px-4 py-10 lg:grid-cols-[1fr_26rem]">
          <div className="hidden max-w-lg text-white lg:block">
            <p className="text-xs font-medium uppercase tracking-[0.28em] text-emerald-100/90">Harvest &amp; Co.</p>
            <h1 className="mt-4 text-4xl font-semibold leading-tight text-white drop-shadow-sm">
              Organic food, directly from trusted farms.
            </h1>
            <p className="mt-4 max-w-md text-base leading-7 text-emerald-50/90">
              Sign in to checkout faster, follow orders, and keep your cart as you move between devices.
            </p>
          </div>
          <div className="mx-auto w-full max-w-md rounded-2xl border border-white/50 bg-[#f8f6f1]/95 p-6 shadow-[0_24px_80px_rgba(6,46,28,0.35)] backdrop-blur-md sm:p-8">
            <p className="mb-2 text-xs font-medium uppercase tracking-[0.18em] text-emerald-700 lg:hidden">Harvest &amp; Co.</p>
            <h2 className="text-2xl font-semibold text-emerald-900">{title}</h2>
            <p className="mt-1 text-sm text-slate-600">{subtitle}</p>
            <div className="mt-6">{children}</div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <section className="mx-auto grid max-w-5xl overflow-hidden rounded-2xl border border-emerald-100 bg-white shadow-sm md:grid-cols-2">
      <div className="relative hidden min-h-[28rem] flex-col justify-end overflow-hidden bg-emerald-900 p-10 text-emerald-50 md:flex">
        <img
          className="absolute inset-0 h-full w-full object-cover"
          src={LOGIN_SCENE_SRC}
          alt=""
          aria-hidden="true"
        />
        <div className="absolute inset-0 bg-linear-to-t from-emerald-950/90 via-emerald-950/55 to-emerald-950/20" />
        <div className="relative">
          <p className="text-xs font-medium uppercase tracking-[0.22em] text-emerald-200">Harvest &amp; Co.</p>
          <h1 className="mt-4 text-3xl font-semibold leading-tight">Organic food, directly from trusted farms.</h1>
          <p className="mt-3 max-w-sm text-sm leading-6 text-emerald-100/85">
            Sign in to checkout faster, follow orders, and keep your cart as you move between devices.
          </p>
        </div>
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
