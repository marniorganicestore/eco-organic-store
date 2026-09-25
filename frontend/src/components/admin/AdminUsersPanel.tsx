import { useState } from 'react'
import { ApiError } from '../../lib/api'
import { ADMIN_ROLE, CUSTOMER_ROLE, isAdmin, roleLabel } from '../../lib/userDisplay'
import { useAuthStore } from '../../store/authStore'
import { useAdminAccounts, useUpdateAccountAccess } from '../../hooks/useAdminUsers'
import { Pager } from '../layout/Pager'
import { useClampPage } from '../../hooks/useClampPage'
import { storeBtnGhost, storeCard } from '../layout/PageShell'
import { FormBanner } from '../account/FormBanner'
import { UserAvatar } from '../account/UserAvatar'

function roleChip(selected: boolean): string {
  return `rounded-md px-3 py-1.5 text-sm font-medium outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 ${
    selected ? 'bg-emerald-700 text-white' : 'text-emerald-900 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-40'
  }`
}

export function AdminUsersPanel() {
  const currentUserId = useAuthStore((state) => state.user?.userId)
  const [page, setPage] = useState(0)
  const accounts = useAdminAccounts(page)
  const people = accounts.data?.items ?? []
  useClampPage(page, accounts.data?.totalPages, setPage)
  const update = useUpdateAccountAccess()
  const [error, setError] = useState('')
  const pendingId = update.isPending ? update.variables?.userId : undefined

  function changeRole(userId: string, admin: boolean) {
    setError('')
    update.mutate(
      { userId, roles: admin ? [CUSTOMER_ROLE, ADMIN_ROLE] : [CUSTOMER_ROLE] },
      { onError: (err) => setError(err instanceof ApiError ? err.message : 'Unable to update that role.') }
    )
  }

  function changeStatus(userId: string, enabled: boolean) {
    setError('')
    update.mutate(
      { userId, enabled },
      { onError: (err) => setError(err instanceof ApiError ? err.message : 'Unable to update that account.') }
    )
  }

  return (
    <section className={`${storeCard} p-4`}>
      <p className="max-w-2xl text-sm text-slate-600">Admins can still shop. You cannot remove your own admin access or disable your own account.</p>
      {error ? <div className="mt-3"><FormBanner tone="error">{error}</FormBanner></div> : null}
      {accounts.isPending ? (
        <div className="mt-4 space-y-3" aria-busy="true">
          <p className="sr-only">Loading accounts...</p>
          <div className="h-16 animate-pulse rounded-xl bg-emerald-100/80" />
          <div className="h-16 animate-pulse rounded-xl bg-emerald-100/80" />
        </div>
      ) : null}
      {accounts.isError ? (
        <div className="mt-3">
          <FormBanner tone="error">Unable to load accounts. Refresh and try again.</FormBanner>
        </div>
      ) : null}
      {accounts.data && people.length === 0 ? (
        <p className="mt-4 text-sm text-slate-600">No accounts yet. Customers appear here after they register.</p>
      ) : null}
      {people.length > 0 ? (
        <ul className="mt-4 space-y-3">
          {people.map((account) => {
            const admin = isAdmin(account.roles)
            const self = account.userId === currentUserId
            const pending = pendingId === account.userId
            const label = account.name || account.email
            return (
              <li key={account.userId} className="rounded-xl border border-emerald-100 bg-white/75 p-3">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 items-center gap-3">
                    <UserAvatar name={label} avatar={account.avatar} />
                    <div className="min-w-0">
                      <p className="truncate font-medium text-emerald-950">
                        {label}
                        {self ? <span className="ml-2 text-xs font-medium text-emerald-800">You</span> : null}
                      </p>
                      <p className="truncate text-sm text-slate-600">{account.email}</p>
                      <p className={`text-xs ${account.enabled ? 'text-emerald-800' : 'text-orange-800'}`}>
                        {account.roles.map(roleLabel).join(' · ')} · {account.enabled ? 'Active' : 'Disabled'}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <div className="inline-flex rounded-lg border border-emerald-200 bg-white p-0.5" role="group" aria-label={`Role for ${label}`}>
                      <button
                        type="button"
                        className={roleChip(!admin)}
                        aria-pressed={!admin}
                        disabled={pending || !admin || self}
                        title={self ? 'You cannot remove your own admin access.' : undefined}
                        onClick={() => changeRole(account.userId, false)}
                      >
                        Customer
                      </button>
                      <button
                        type="button"
                        className={roleChip(admin)}
                        aria-pressed={admin}
                        disabled={pending || admin}
                        onClick={() => changeRole(account.userId, true)}
                      >
                        Admin
                      </button>
                    </div>
                    <button
                      type="button"
                      className={storeBtnGhost}
                      disabled={pending || (self && account.enabled)}
                      title={self && account.enabled ? 'You cannot disable your own account.' : undefined}
                      onClick={() => changeStatus(account.userId, !account.enabled)}
                    >
                      {pending ? 'Saving...' : account.enabled ? 'Disable' : 'Enable'}
                    </button>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      ) : null}
      {accounts.data ? (
        <Pager
          page={accounts.data.page}
          size={accounts.data.size}
          totalElements={accounts.data.totalElements}
          totalPages={accounts.data.totalPages}
          onPage={setPage}
          label="Account pages"
        />
      ) : null}
    </section>
  )
}
