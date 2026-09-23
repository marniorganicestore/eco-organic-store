#Requires -Version 7.0
<#
.SYNOPSIS
  Bind eco-organic-store.com and api.eco-organic-store.com after GoDaddy CNAMEs exist.
#>
param(
    [string] $ResourceGroup = 'rg-harvest-prod',
    [string] $SwaName = 'swa-harvest',
    [string] $GatewayName = 'gateway',
    [string] $EnvironmentName = 'cae-harvest',
    [string] $Apex = 'eco-organic-store.com'
)

$ErrorActionPreference = 'Stop'
$env:PATH = 'C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin;' + $env:PATH

$www = "www.$Apex"
$api = "api.$Apex"
$swaHost = az staticwebapp show -n $SwaName -g $ResourceGroup --query defaultHostname -o tsv
$gwHost = az containerapp show -n $GatewayName -g $ResourceGroup --query properties.configuration.ingress.fqdn -o tsv

function Wait-Cname([string]$Name, [string]$Expected) {
    Write-Host "Waiting for CNAME $Name -> $Expected"
    $deadline = (Get-Date).AddMinutes(15)
    do {
        $got = $null
        try {
            $got = (Resolve-DnsName $Name -Type CNAME -ErrorAction Stop | Where-Object { $_.Type -eq 'CNAME' } | Select-Object -First 1).NameHost
        } catch { }
        if ($got -and $got.TrimEnd('.') -eq $Expected.TrimEnd('.')) {
            Write-Host "  resolved $got"
            return
        }
        Write-Host "  now=$got"
        Start-Sleep -Seconds 20
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for CNAME $Name"
}

Wait-Cname -Name $www -Expected $swaHost
Wait-Cname -Name $api -Expected $gwHost

Write-Host "Binding Static Web App hostnames"
az staticwebapp hostname set -n $SwaName -g $ResourceGroup --hostname $www --output none
try {
    az staticwebapp hostname set -n $SwaName -g $ResourceGroup --hostname $Apex --output none
} catch {
    Write-Host "Apex hostname not bound yet ($($_.Exception.Message)). www is enough if GoDaddy forwards @ -> www."
}

Write-Host "Binding Container App api.$Apex + managed cert"
az containerapp hostname add -n $GatewayName -g $ResourceGroup --hostname $api --output none
az containerapp hostname bind -n $GatewayName -g $ResourceGroup --hostname $api --environment $EnvironmentName --validation-method CNAME --output none

Write-Host "Setting GitHub repo variables (triggers a new SPA bake on next Azure workflow)"
gh variable set STOREFRONT_URL --body "https://$Apex" --repo marniorganicestore/eco-organic-store
gh variable set VITE_API_BASE --body "https://$api" --repo marniorganicestore/eco-organic-store

Write-Host "Disabling GitHub Pages so it no longer claims the domain"
gh api --method DELETE repos/marniorganicestore/eco-organic-store/pages 2>$null

Write-Host ""
Write-Host "Custom domains bound."
Write-Host "  Storefront: https://$www  and https://$Apex"
Write-Host "  API:        https://$api"
Write-Host "Re-run Actions → Azure so the SPA is built with VITE_API_BASE=https://$api"
