/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE?: string
  readonly VITE_GOOGLE_CLIENT_ID?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

interface GoogleIdentityCredential {
  credential?: string
}

interface GoogleIdentityApi {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string
        callback: (response: GoogleIdentityCredential) => void
      }) => void
      renderButton: (
        parent: HTMLElement,
        options: {
          theme?: string
          size?: string
          width?: number
          text?: string
          shape?: string
        }
      ) => void
    }
  }
}

interface Window {
  google?: GoogleIdentityApi
}
