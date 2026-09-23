import { AdminUsersPanel } from '../../components/admin/AdminUsersPanel'
import { PageShell } from '../../components/layout/PageShell'

export default function AdminPeoplePage() {
  return (
    <PageShell title="People" subtitle="Registration always creates a customer. Grant admin here when someone should run the store.">
      <AdminUsersPanel />
    </PageShell>
  )
}
