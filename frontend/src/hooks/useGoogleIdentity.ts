import { useEffect, useRef } from 'react'

const GIS_SCRIPT = 'https://accounts.google.com/gsi/client'

let gisLoader: Promise<void> | null = null

function loadGoogleIdentity(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (!gisLoader) {
    gisLoader = new Promise((resolve, reject) => {
      const existing = document.querySelector<HTMLScriptElement>('script[data-store-gis]')
      if (existing) {
        existing.addEventListener('load', () => resolve(), { once: true })
        existing.addEventListener('error', () => reject(new Error('Unable to load Google sign-in')), { once: true })
        return
      }
      const script = document.createElement('script')
      script.src = GIS_SCRIPT
      script.async = true
      script.dataset.storeGis = 'true'
      script.onload = () => resolve()
      script.onerror = () => reject(new Error('Unable to load Google sign-in'))
      document.head.appendChild(script)
    })
  }
  return gisLoader
}

export function useGoogleIdentity(
  clientId: string | undefined,
  onCredential: (idToken: string) => void,
  disabled: boolean
) {
  const containerRef = useRef<HTMLDivElement>(null)
  const callbackRef = useRef(onCredential)
  callbackRef.current = onCredential

  useEffect(() => {
    if (!clientId || disabled) return
    let cancelled = false

    loadGoogleIdentity()
      .then(() => {
        if (cancelled || !containerRef.current || !window.google?.accounts?.id) return
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => {
            if (response.credential) callbackRef.current(response.credential)
          }
        })
        containerRef.current.replaceChildren()
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          width: Math.max(containerRef.current.offsetWidth, 280),
          text: 'continue_with',
          shape: 'rectangular'
        })
      })
      .catch(() => {
        if (!cancelled && containerRef.current) {
          containerRef.current.textContent = 'Google sign-in could not be loaded.'
        }
      })

    return () => {
      cancelled = true
    }
  }, [clientId, disabled])

  return containerRef
}
