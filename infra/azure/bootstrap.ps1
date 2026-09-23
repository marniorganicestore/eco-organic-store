#Requires -Version 7.0
<#
.SYNOPSIS
  One-time Azure + GitHub OIDC bootstrap for Eco Organic Store

.DESCRIPTION
  Creates the resource group and an Entra app with a federated credential
  bound to GitHub environment "azure". Prints the three secrets to paste
  into GitHub → Settings → Environments → azure.

  Does not deploy Container Apps or store JWT/Stripe material.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $SubscriptionId,

    [Parameter(Mandatory = $true)]
    [string] $GitHubRepo,

    [string] $ResourceGroup = 'rg-harvest-prod',
    [string] $Location = 'centralindia',
    [string] $AppName = 'github-harvest-azure'
)

$ErrorActionPreference = 'Stop'

# Accept owner/repo or a github.com URL (with or without .git).
$GitHubRepo = $GitHubRepo.Trim() -replace '^https?://github\.com/', '' -replace '\.git$', '' -replace '/+$', ''
if ($GitHubRepo -notmatch '^[^/]+/[^/]+$') {
    throw "GitHubRepo must be 'owner/repo' (example: owner/eco-organic-store). Got: $GitHubRepo"
}

az account set --subscription $SubscriptionId
if ($LASTEXITCODE -ne 0) { throw "az account set failed. Run az login first." }

Write-Host "Creating resource group $ResourceGroup in $Location"
az group create --name $ResourceGroup --location $Location --output none

$appId = az ad app list --display-name $AppName --query '[0].appId' --output tsv
if (-not $appId) {
    Write-Host "Creating Entra app $AppName"
    $appId = az ad app create --display-name $AppName --query appId --output tsv
}

$spId = az ad sp list --filter "appId eq '$appId'" --query '[0].id' --output tsv
if (-not $spId) {
    Write-Host "Creating service principal"
    $spId = az ad sp create --id $appId --query id --output tsv
}

foreach ($role in @('Contributor', 'User Access Administrator')) {
    $existing = az role assignment list --assignee $spId --role $role --scope "/subscriptions/$SubscriptionId/resourceGroups/$ResourceGroup" --query '[0].id' --output tsv
    if (-not $existing) {
        Write-Host "Assigning $role on $ResourceGroup"
        az role assignment create --assignee-object-id $spId --assignee-principal-type ServicePrincipal --role $role --scope "/subscriptions/$SubscriptionId/resourceGroups/$ResourceGroup" --output none
    }
}

function Add-FederatedCredential([string]$Name, [string]$Subject) {
    $existing = az ad app federated-credential list --id $appId --query "[?name=='$Name' || subject=='$Subject'].id" --output tsv
    if ($existing) {
        return
    }
    Write-Host "Creating federated credential $Name ($Subject)"
    $fedFile = Join-Path ([System.IO.Path]::GetTempPath()) "$Name.json"
    @{
        name        = $Name
        issuer      = 'https://token.actions.githubusercontent.com'
        subject     = $Subject
        description = 'GitHub Actions environment azure'
        audiences   = @('api://AzureADTokenExchange')
    } | ConvertTo-Json | Set-Content -Path $fedFile -Encoding utf8
    az ad app federated-credential create --id $appId --parameters $fedFile --output none
    Remove-Item $fedFile -ErrorAction SilentlyContinue
}

Add-FederatedCredential -Name 'github-environment-azure' -Subject "repo:${GitHubRepo}:environment:azure"

$owner, $repoName = $GitHubRepo.Split('/')
$ownerId = $null
$repoId = $null
if (Get-Command gh -ErrorAction SilentlyContinue) {
    $repoId = gh api "repos/$GitHubRepo" --jq .id 2>$null
    $ownerId = gh api "orgs/$owner" --jq .id 2>$null
    if (-not $ownerId) {
        $ownerId = gh api "users/$owner" --jq .id 2>$null
    }
}
if ($ownerId -and $repoId) {
    Add-FederatedCredential -Name 'github-environment-azure-ids' -Subject "repo:${owner}@${ownerId}/${repoName}@${repoId}:environment:azure"
}

$tenantId = az account show --query tenantId --output tsv

Write-Host ""
Write-Host "Create GitHub environment 'azure' and add these secrets:"
Write-Host "  AZURE_CLIENT_ID       = $appId"
Write-Host "  AZURE_TENANT_ID       = $tenantId"
Write-Host "  AZURE_SUBSCRIPTION_ID = $SubscriptionId"
Write-Host ""
Write-Host "Also add application secrets on that environment:"
Write-Host "  JWT_SECRET, INTERNAL_API_KEY, ADMIN_PASSWORD"
Write-Host "  STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET, GOOGLE_CLIENT_ID"
Write-Host ""
Write-Host "Optional repo variables:"
Write-Host "  AZURE_RESOURCE_GROUP = $ResourceGroup"
Write-Host "  AZURE_LOCATION       = $Location"
Write-Host "  STOREFRONT_URL       = https://eco-organic-store.com"
Write-Host "  VITE_API_BASE        = https://api.eco-organic-store.com"
Write-Host ""
Write-Host "Service principal object id (Bicep deployerPrincipalId) = $spId"
