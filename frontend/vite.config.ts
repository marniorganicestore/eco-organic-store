import { copyFileSync, existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, type Plugin } from 'vite'

function normalizeBase(raw: string | undefined): string {
  if (!raw || raw === '/') {
    return '/'
  }
  return raw.endsWith('/') ? raw : `${raw}/`
}

/**
 * GitHub Pages has no SPA rewrite. Copy index.html onto known routes so
 * /shop (and friends) return 200, and keep 404.html for /product/:slug.
 */
const SPA_FALLBACK_PAGES = [
  '404.html',
  'shop/index.html',
  'cart/index.html',
  'checkout/index.html',
  'login/index.html',
  'register/index.html',
  'admin/index.html',
  'account/orders/index.html',
  'order/success/index.html'
]

function githubPagesSpaFallback(): Plugin {
  return {
    name: 'github-pages-spa-fallback',
    writeBundle() {
      const indexHtml = resolve('dist/index.html')
      if (!existsSync(indexHtml)) {
        return
      }
      writeFileSync(resolve('dist/.nojekyll'), '')
      for (const page of SPA_FALLBACK_PAGES) {
        const target = resolve('dist', page)
        mkdirSync(dirname(target), { recursive: true })
        copyFileSync(indexHtml, target)
      }
    }
  }
}

export default defineConfig({
  base: normalizeBase(process.env.VITE_BASE_PATH),
  plugins: [react(), tailwindcss(), githubPagesSpaFallback()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
