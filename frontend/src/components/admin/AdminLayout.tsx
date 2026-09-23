import { NavLink, Outlet } from 'react-router-dom'
import { storeCard } from '../layout/PageShell'

const links = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/catalog', label: 'Catalog', end: false },
  { to: '/admin/inventory', label: 'Inventory', end: false },
  { to: '/admin/orders', label: 'Orders', end: false },
  { to: '/admin/payments', label: 'Payments', end: false },
  { to: '/admin/reviews', label: 'Reviews', end: false },
  { to: '/admin/people', label: 'People', end: false }
]

export function AdminLayout() {
  return (
    <div className="grid gap-6 lg:grid-cols-[220px_minmax(0,1fr)]">
      <nav aria-label="Admin" className={`${storeCard} h-fit p-2`}>
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
