type FormBannerProps = {
  tone: 'error' | 'success'
  children: string
}

export function FormBanner({ tone, children }: FormBannerProps) {
  if (tone === 'error') {
    return (
      <p role="alert" className="rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-800">
        {children}
      </p>
    )
  }
  return (
    <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-900">
      {children}
    </p>
  )
}
