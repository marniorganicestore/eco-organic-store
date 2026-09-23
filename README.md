# Eco Organic Store (Microservices)

[![CI](https://github.com/marniorganicestore/eco-organic-store/actions/workflows/ci.yml/badge.svg)](https://github.com/marniorganicestore/eco-organic-store/actions/workflows/ci.yml)
[![Azure](https://github.com/marniorganicestore/eco-organic-store/actions/workflows/azure.yml/badge.svg)](https://github.com/marniorganicestore/eco-organic-store/actions/workflows/azure.yml)
[![Docker](https://github.com/marniorganicestore/eco-organic-store/actions/workflows/docker.yml/badge.svg)](https://github.com/marniorganicestore/eco-organic-store/actions/workflows/docker.yml)

Full-stack organic e-store built with Spring Boot 4.1 microservices, MongoDB, and React 19.

## Tech stack

- Java 21, Maven multi-module
- Spring Boot 4.1.1, Spring Cloud 2025.1.3, Gateway WebMVC
- MongoDB 8 (database-per-service)
- React 19.3, Vite 8, Tailwind CSS 4
- Stripe Checkout (test mode)

## Services

- gateway: `8080`
- identity-service: `8081`
- catalog-service: `8082`
- cart-service: `8083`
- inventory-service: `8084`
- order-service: `8085`
- payment-service: `8086`
- review-service: `8087`
- frontend: `5173`

## Local run

Mongo only (then run services on the host as below):

```bash
docker compose up -d mongo
```

Full stack in Docker (same ports: gateway `8080`, storefront `5173`):

```bash
cp .env.example .env
docker compose --profile app up --build
```

### Host processes

1. Start MongoDB with Compose as above.

2. Start backend services (each in a terminal)

```bash
cp .env.example .env
./mvnw -pl gateway spring-boot:run
./mvnw -pl identity-service spring-boot:run
./mvnw -pl catalog-service spring-boot:run
./mvnw -pl cart-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl payment-service spring-boot:run
./mvnw -pl review-service spring-boot:run
```

3. Start frontend

```bash
cd frontend
npm install
npm run dev
```

## Key flows

- Register/login (JWT access + refresh cookie)
- Browse categories/products, add to cart
- Checkout via payment session URL
- Payment webhook marks order paid and confirms inventory
- Verified purchasers can post reviews
- Admin endpoints for catalog, inventory, orders, payments, reviews

## CI / CD

There is no Kubernetes target in v1. **App CD is: squash-merge a green PR into `main`.** Direct pushes to `main` should be blocked by the ruleset below.

Production is **Azure** (not GitHub Pages). On push to `main`, `.github/workflows/azure.yml` logs in with OIDC, deploys `infra/azure`, pushes Java images to Azure Container Registry, updates the eight Container Apps, and publishes the Vite `dist` to Azure Static Web Apps. One-time subscription setup: [`infra/azure/README.md`](infra/azure/README.md).

```text
Browser  https://eco-organic-store.com     Azure Static Web Apps
              │
              ▼
         https://api.eco-organic-store.com  Container App gateway (public)
              │
              ▼
         identity · catalog · cart · inventory · order · payment · review
         (Container Apps, internal ingress)
              │
              ▼
         Cosmos DB for MongoDB (serverless) — one account, seven databases
```

The gateway is the only public API. Domain services are not on the internet. Secrets live in Azure Key Vault; GitHub environment `azure` holds OIDC + the values synced on each deploy (`JWT_SECRET`, `INTERNAL_API_KEY`, Stripe, admin seed).

**Site URL:** [https://eco-organic-store.com](https://eco-organic-store.com) (asset `base` is `/`). Set repo variable `VITE_API_BASE` to `https://api.eco-organic-store.com` after the custom domain is on the gateway. Until then the workflow bakes the default `*.azurecontainerapps.io` URL into the SPA. Leave `VITE_API_BASE` unset for local Vite (`/api` proxy). Gateway `CORS_ALLOWED_ORIGINS` must include the live storefront origin.

Pull requests bake images without pushing (`.github/workflows/docker.yml`). Pull requests and pushes to `main` run GitHub Actions:

| Job | What it proves |
|---|---|
| Backend | Java 21, `./mvnw -T 1C verify` across all modules |
| Frontend | `npm ci`, oxlint, TypeScript + Vite production build |
| Compose | `docker-compose.yml` is valid |
| Bicep | `infra/azure/platform.bicep` and `apps.bicep` compile |
| Secrets | Gitleaks CLI scan of the commit graph (`.env.example` allowlisted; no org license) |
| CI | Aggregate gate — **this is the only required status check** |
| Docker | Bake service images on PRs (not required) |
| Azure | OIDC deploy to Container Apps + Static Web Apps on `main` (not a required check) |

CodeQL runs on public clones only (GitHub Advanced Security is required to upload alerts on private repos). Dependabot opens weekly grouped PRs for Maven, npm, Actions, and Compose images. Secrets stay in `.env` (see `.env.example`); they are never committed.

### GitHub ruleset — Protect main

Rulesets on a **private** repo are not enforced until the repo lives under a **GitHub Team** organization. Configure it anyway so enforcement is ready; until then, treat the workflow as policy.

Create **New ruleset → New branch ruleset** and use these values:

| Field | Value |
|---|---|
| Ruleset Name | `Protect main` |
| Enforcement status | **Active** |
| Bypass list | **Empty** (do not add yourself — a bypass skips CI) |
| Target branches | Include **default branch** (`main`) |

**Enable these rules only:**

| Rule | Setting |
|---|---|
| Restrict deletions | On |
| Require linear history | On |
| Require a pull request before merging | On — approvals **0** (solo; raise to 1 when you have a reviewer), dismiss stale reviews **on**, require conversation resolution **on**, code owner review **off**, last-push approval **off**, allowed merge method **squash only** |
| Require status checks to pass | On — required check **`CI`**, require branch to be up to date **on** |
| Block force pushes | On |
| Automatically request Copilot code review | On — review on push **on**, review drafts **off** |

**Leave off:** Restrict creations, Restrict updates, Require deployments, Require signed commits, Require code scanning results, Require code quality results, Restrict code coverage.

Also in **Settings → General → Pull Requests**: allow **squash merge only**, disable merge commits, enable **automatically delete head branches**.

Optional second ruleset **Protect version tags**: target tags `v*`, enable Restrict updates, Restrict deletions, Block force pushes. JSON copies live in `.github/rulesets/`.

## Notes

- Internal APIs are protected with `X-Internal-Key`.
- Gateway blocks `/internal/**` from public access.
- In development without Stripe keys, payment service returns a local success URL.
- Configure `GOOGLE_CLIENT_ID` to enable verified Google sign-in.
- Configure `ADMIN_PASSWORD` (or `ADMIN_PASSWORD_HASH`) to bootstrap the seeded admin user.
- Configure `STRIPE_WEBHOOK_SECRET` in non-local environments to enforce webhook signature verification.