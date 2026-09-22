import { useGoogleIdentity } from '../../hooks/useGoogleIdentity'

type GoogleSignInButtonProps = {
  disabled?: boolean
  onCredential: (idToken: string) => void
}

export function GoogleSignInButton({ disabled = false, onCredential }: GoogleSignInButtonProps) {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim()
  const containerRef = useGoogleIdentity(clientId, onCredential, disabled)

  if (!clientId) return null

  return (
    <div className={disabled ? 'pointer-events-none opacity-50' : undefined}>
      <div className="relative my-5">
        <div className="absolute inset-x-0 top-1/2 border-t border-emerald-100" />
        <p className="relative mx-auto w-fit bg-[#f8f6f1] px-2 text-xs uppercase tracking-wide text-slate-400">or</p>
      </div>
      <div ref={containerRef} className="flex min-h-10 justify-center" aria-label="Continue with Google" />
    </div>
  )
}
