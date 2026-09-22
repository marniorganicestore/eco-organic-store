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
 * Static hosts without a rewrite (and local nginx) still need index.html
 * on known routes. Azure Static Web Apps uses staticwebapp.config.json;
 * 404.html covers /product/:slug on hosts that only map 404 → index.
 */
const SPA_FALLBACK_PAGES = [
  'shop/index.html',
  'cart/index.html',
  'checkout/index.html',
  'login/index.html',
  'register/index.html',
  'forgot-password/index.html',
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
      copyFileSync(indexHtml, resolve('dist/404.html'))
      for (const page of SPA_FALLBACK_PAGES) {
        const target = resolve('dist', page)
        mkdirSync(dirname(target), { recursive: true })
        if (!existsSync(target)) {
          copyFileSync(indexHtml, target)
        }
      }
    }
  }
}

const apiProxy = { '/api': 'http://localhost:8080' }

export default defineConfig({
  base: normalizeBase(process.env.VITE_BASE_PATH),
  plugins: [react(), tailwindcss(), githubPagesSpaFallback()],
  server: {
    proxy: apiProxy
  },
  preview: {
    proxy: apiProxy
  }
})
