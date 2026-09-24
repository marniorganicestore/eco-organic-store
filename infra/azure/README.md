# Azure production (Eco Organic Store)

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

   `GitHubRepo` is `owner/repo` (for example `owner/eco-organic-store`), not a clone URL. If you stay in `pwsh` you can also use backticks for a multi-line call:

```powershell
./infra/azure/bootstrap.ps1 `
  -SubscriptionId '<subscription-guid>' `
  -GitHubRepo '<owner>/<repo>'
```

3. GitHub → **Settings → Environments → New environment** named `azure`.
4. Paste the three OIDC secrets the script prints (`AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`).
5. Add application secrets on that same environment: `JWT_SECRET`, `INTERNAL_API_KEY`, `ADMIN_PASSWORD`, `MAIL_PASSWORD`, and optionally `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `GOOGLE_CLIENT_ID`.
6. Optional repo **variables**: `AZURE_RESOURCE_GROUP`, `AZURE_LOCATION`, `STOREFRONT_URL`, `VITE_API_BASE`, `MAIL_ENABLED`, `MAIL_HOST`.
7. Run **Actions → Azure → Run workflow**.

## Custom domains (required for reliable login)

Nameservers are GoDaddy (`ns59` / `ns60.domaincontrol.com`). Today the apex still points at GitHub Pages (`185.199.x`). Replace those records, then bind Azure.

Create a **production** API key at [developer.godaddy.com/keys](https://developer.godaddy.com/keys) and run:

```powershell
$verificationId = az containerapp env show -g rg-harvest-prod -n cae-harvest --query properties.customDomainVerificationId -o tsv
pwsh -File ./infra/azure/godaddy-dns.ps1 -ApiKey '<key>' -ApiSecret '<secret>' -ApiVerificationId $verificationId
pwsh -File ./infra/azure/bind-custom-domains.ps1
```

Or paste this in GoDaddy → DNS (delete the GitHub Pages `A` / `AAAA` on `@` and the `www` CNAME to `marniorganicestore.github.io`). Use the same verification id for the TXT record:

| Type | Name | Value | TTL |
|---|---|---|---|
| CNAME | `www` | `orange-smoke-074631600.3.azurestaticapps.net` | 600 |
| CNAME | `@` | `orange-smoke-074631600.3.azurestaticapps.net` | 600 |
| CNAME | `api` | `gateway.redforest-7e8aefa3.centralindia.azurecontainerapps.io` | 600 |
| TXT | `asuid.api` | `<customDomainVerificationId>` | 600 |

GoDaddy often rejects a CNAME on `@`. If it does, leave `@` as a **301 forward** to `https://www.eco-organic-store.com`. Then run `bind-custom-domains.ps1` (it waits for DNS, attaches managed TLS, sets `STOREFRONT_URL` / `VITE_API_BASE`, and turns off GitHub Pages).

Re-run **Actions → Azure** after bind so the SPA is compiled against `https://api.eco-organic-store.com`. Razorpay webhook: `https://api.eco-organic-store.com/api/webhooks/razorpay`.

## Store email

Mail is sent by `identity-service` from `admin@eco-organic-store.com`. Order updates are copied to that mailbox. Password links go only to the customer. `STOREFRONT_URL` is the link host.

Until `MAIL_ENABLED` is `true`, identity writes each message to its log and does not open SMTP.

1. Create the mailbox `admin@eco-organic-store.com` and an SMTP password (Microsoft 365 app password, or the GoDaddy mailbox password). Submission is port 587 with STARTTLS.
2. GitHub → **Settings → Environments → azure → Secrets**: `MAIL_PASSWORD` = that SMTP password.
3. GitHub → **Settings → Secrets and variables → Actions → Variables**:
   - `MAIL_ENABLED` = `true`
   - `MAIL_HOST` = the provider host (`smtp.office365.com` for Microsoft 365, `smtpout.secureserver.net` for GoDaddy Workspace Email)
   - `STOREFRONT_URL` = `https://eco-organic-store.com` so reset links hit the shop, not the `azurestaticapps.net` hostname
4. Merge to `main` (or **Actions → Azure → Run workflow**). The workflow stores `mail-password` in Key Vault and restarts identity with those settings.
5. Place a test order and request a password reset. Confirm the message arrives from `admin@eco-organic-store.com`. Add the provider SPF and DKIM records in GoDaddy DNS so inboxes do not junk the mail.

Container Apps egress already allows outbound 587. Do not put the mailbox password in Bicep or in the repo.

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
