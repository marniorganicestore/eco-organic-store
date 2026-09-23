import { Link } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

const HOME_SCENE_SRC = '/images/home-hero.png'

export function HomeHero() {
  const user = useAuthStore((state) => state.user)

  return (
    <section className="relative isolate min-h-[calc(100vh-4.75rem)] overflow-hidden">
      <img
        src={HOME_SCENE_SRC}
        alt=""
        width={1920}
        height={1080}
        decoding="async"
        fetchPriority="high"
        aria-hidden="true"
        className="absolute inset-0 h-full w-full scale-105 object-cover object-center"
      />
      <div className="absolute inset-0 bg-linear-to-r from-emerald-950/88 via-emerald-950/55 to-emerald-950/20" />
      <div className="absolute inset-x-0 bottom-0 h-36 bg-linear-to-t from-emerald-950/55 to-transparent" />
      <div className="relative mx-auto flex min-h-[calc(100vh-4.75rem)] max-w-6xl items-center px-4 py-16">
        <div className="max-w-xl text-white">
          <p className="text-xs font-medium uppercase tracking-[0.28em] text-emerald-100/90">Eco Organic Store</p>
          <h1 className="mt-4 text-4xl font-semibold leading-tight drop-shadow-sm sm:text-5xl">
            Organic food, directly from trusted farms.
          </h1>
          <p className="mt-4 max-w-md text-base leading-7 text-emerald-50/90">
            Fresh produce, pantry staples, and dairy curated for clean living.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to="/shop"
              className="rounded-lg bg-emerald-600 px-5 py-2.5 font-medium text-white shadow-lg shadow-emerald-950/30 hover:bg-emerald-500"
            >
              Shop now
            </Link>
            {user ? null : (
              <Link
                to="/login"
                className="rounded-lg border border-white/40 bg-white/10 px-5 py-2.5 font-medium text-white backdrop-blur-sm hover:bg-white/20"
              >
                Sign in
              </Link>
            )}
          </div>
        </div>
      </div>
    </section>
  )
}
