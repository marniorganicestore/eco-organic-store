# Azure production (Harvest & Co.)

Hosted topology:

```text
https://eco-organic-store.com          Azure Static Web Apps (React dist)
https://api.eco-organic-store.com      Container App `gateway` (external)
         │
         ├── identity-service … review-service   Container Apps (internal)
         └── Cosmos DB for MongoDB (serverless)  one account, seven databases
```

Local development is unchanged (`docker-compose.yml` + Mongo 8). This folder is the hosted target only. No AKS, no EC2, no new Java modules. If Cosmos serverless or Container Apps is unavailable in `centralindia`, set repo variable `AZURE_LOCATION` to `eastus` and redeploy.

Orgs that include numeric IDs in the GitHub OIDC `sub` claim need both federated credentials (`owner/repo` and `owner@id/repo@id`). `bootstrap.ps1` creates both when `gh` is available.

## One-time bootstrap

1. Azure CLI logged in, Owner or User Access Administrator on the subscription.
2. From the repo root, in **PowerShell 7** (`pwsh`). Windows PowerShell 5.1 cannot run this script.

   Open a new terminal and type `pwsh`, or call it from any prompt:

```powershell
pwsh -File ./infra/azure/bootstrap.ps1 -SubscriptionId '<subscription-guid>' -GitHubRepo '<owner>/<repo>'
```

   `GitHubRepo` is `owner/repo` (for example `marniorganicestore/harvest-co`), not a clone URL. If you stay in `pwsh` you can also use backticks for a multi-line call:

```powershell
./infra/azure/bootstrap.ps1 `
  -SubscriptionId '<subscription-guid>' `
  -GitHubRepo '<owner>/<repo>'
```

3. GitHub → **Settings → Environments → New environment** named `azure`.
4. Paste the three OIDC secrets the script prints (`AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`).
5. Add application secrets on that same environment: `JWT_SECRET`, `INTERNAL_API_KEY`, `ADMIN_PASSWORD`, and optionally `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `GOOGLE_CLIENT_ID`.
6. Optional repo **variables**: `AZURE_RESOURCE_GROUP`, `AZURE_LOCATION`, `STOREFRONT_URL`, `VITE_API_BASE`.
7. Run **Actions → Azure → Run workflow**.

## Custom domains (required for reliable login)

Until DNS moves, the workflow uses the default `*.azurestaticapps.net` and `*.azurecontainerapps.io` hosts. Those are **cross-site**, so the refresh cookie is `SameSite=None; Secure`. Chrome still partitions third-party cookies — attach the real hostnames before you treat auth as production.

Turn off GitHub Pages after the first successful Azure deploy (Settings → Pages → None) so `eco-organic-store.com` is not still served from the old static site.

| Host | Azure resource | DNS |
|---|---|---|
| `eco-organic-store.com` / `www` | Static Web App `swa-harvest` | CNAME to the SWA default host (remove the GitHub Pages record) |
| `api.eco-organic-store.com` | Container App `gateway` | Container Apps custom domain + CNAME |

Then set repo variables:

- `STOREFRONT_URL=https://eco-organic-store.com`
- `VITE_API_BASE=https://api.eco-organic-store.com`

Stripe webhook endpoint: `https://api.eco-organic-store.com/api/webhooks/stripe`.

## What GitHub Actions deploys

`/.github/workflows/azure.yml` on `main`:

1. OIDC login (no JSON key).
2. `platform.bicep` — ACR, Container Apps environment, Cosmos Mongo serverless (7 DBs), Key Vault, Static Web App, pull/push identities.
3. Sync GitHub environment secrets into Key Vault.
4. Bake the existing module images and push to ACR.
5. `apps.bicep` — eight Container Apps. Gateway is the only public ingress.
6. Build the SPA with `VITE_API_BASE` and upload to Static Web Apps.

## Cost shape (central India / consumption)

Scale-to-zero Java plus serverless Cosmos is typically tens of dollars a month for a quiet shop, not zero. First request after idle is a Java cold start. Set GitHub variable `GATEWAY_MIN_REPLICAS=1` if the storefront must not wait on the gateway.
