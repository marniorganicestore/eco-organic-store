#Requires -Version 7.0
<#
.SYNOPSIS
  Point eco-organic-store.com at Azure from GoDaddy DNS.

.DESCRIPTION
  Replaces GitHub Pages records with:
    www  CNAME  orange-smoke-074631600.3.azurestaticapps.net
    @    CNAME  same (GoDaddy flattens the apex) — falls back to 301 → www
    api  CNAME  gateway.redforest-7e8aefa3.centralindia.azurecontainerapps.io
    asuid.api TXT  Container Apps domain verification id

  Create a production key at https://developer.godaddy.com/keys
  (environment: production, not OTE).
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $ApiKey,

    [Parameter(Mandatory = $true)]
    [string] $ApiSecret,

    [string] $Domain = 'eco-organic-store.com',
    [string] $StorefrontTarget = 'orange-smoke-074631600.3.azurestaticapps.net',
    [string] $GatewayTarget = 'gateway.redforest-7e8aefa3.centralindia.azurecontainerapps.io',
    [string] $ApiVerificationId = 'A17E344185B44624340C851146A6077ED21CB66DDC93C1C2BD0ED0CE313ECB83'
)

$ErrorActionPreference = 'Stop'
$headers = @{
    Authorization = "sso-key ${ApiKey}:${ApiSecret}"
    Accept        = 'application/json'
    'Content-Type' = 'application/json'
}
$base = "https://api.godaddy.com/v1/domains/$Domain"

function Invoke-GoDaddy {
    param([string]$Method, [string]$Path, $Body)
    $uri = "$base$Path"
    $params = @{ Uri = $uri; Method = $Method; Headers = $headers }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Compress -Depth 6)
    }
    Invoke-RestMethod @params
}

function Set-Record([string]$Type, [string]$Name, [string]$Data, [int]$Ttl = 600) {
    Write-Host "PUT $Type $Name -> $Data"
    Invoke-GoDaddy -Method Put -Path "/records/$Type/$Name" -Body @(
        @{ data = $Data; ttl = $Ttl }
    )
}

Write-Host "Updating DNS for $Domain"

Set-Record -Type CNAME -Name www -Data $StorefrontTarget
Set-Record -Type CNAME -Name api -Data $GatewayTarget
Set-Record -Type TXT -Name asuid.api -Data $ApiVerificationId

$apexCnameOk = $true
try {
    Write-Host "Clearing GitHub Pages A/AAAA at apex so a CNAME can be set"
    Invoke-GoDaddy -Method Put -Path '/records/A/@' -Body @()
    Invoke-GoDaddy -Method Put -Path '/records/AAAA/@' -Body @()
    Set-Record -Type CNAME -Name '@' -Data $StorefrontTarget
} catch {
    $apexCnameOk = $false
    Write-Host "Apex CNAME rejected ($($_.Exception.Message)). Forwarding $Domain -> https://www.$Domain"
    $fwd = @{
        type = 'REDIRECT_PERMANENT'
        url  = "https://www.$Domain"
    }
    Invoke-RestMethod -Method Put -Uri "$base/forwarding" -Headers $headers -Body ($fwd | ConvertTo-Json -Compress)
}

Write-Host ""
Write-Host "GoDaddy DNS updated. Apex CNAME applied: $apexCnameOk"
Write-Host "Wait 2-10 minutes, then run:"
Write-Host "  pwsh -File ./infra/azure/bind-custom-domains.ps1"
