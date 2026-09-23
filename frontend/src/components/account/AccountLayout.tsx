import { NavLink, Outlet } from 'react-router-dom'
import { harvestCard } from '../layout/PageShell'

const links = [
  { to: '/account', label: 'Profile', end: true },
  { to: '/account/addresses', label: 'Addresses', end: false },
  { to: '/account/orders', label: 'Orders', end: false },
  { to: '/account/security', label: 'Security', end: false }
]

export function AccountLayout() {
  return (
    <div className="grid gap-6 lg:grid-cols-[220px_minmax(0,1fr)]">
      <nav aria-label="Account" className={`${harvestCard} h-fit p-2`}>
        <ul className="flex gap-1 overflow-x-auto lg:flex-col">
          {links.map((link) => (
            <li key={link.to}>
              <NavLink
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  `block whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 ${
                    isActive ? 'bg-emerald-800 text-white' : 'text-emerald-950 hover:bg-emerald-50'
                  }`
                }
              >
                {link.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div>
        <Outlet />
      </div>
    </div>
  )
}

export function AccountSkeleton() {
  return (
    <div className={`${harvestCard} animate-pulse space-y-3 p-6`} aria-hidden="true">
      <div className="h-16 w-16 rounded-full bg-emerald-100" />
      <div className="h-5 w-40 rounded bg-emerald-100" />
      <div className="h-4 w-64 rounded bg-emerald-50" />
      <div className="h-24 rounded-xl bg-emerald-50" />
    </div>
  )
}
