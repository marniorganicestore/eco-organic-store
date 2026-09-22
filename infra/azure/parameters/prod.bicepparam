using '../platform.bicep'

param location = 'centralindia'
param swaLocation = 'eastasia'
param webOrigins = 'http://localhost:5173,https://eco-organic-store.com,https://www.eco-organic-store.com'

// Passed at deploy time from the GitHub OIDC app object id.
// param deployerPrincipalId = '<entra-object-id>'
