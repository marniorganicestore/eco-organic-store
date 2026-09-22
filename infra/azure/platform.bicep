@description('Azure region for ACR, Container Apps, Cosmos, and Key Vault.')
param location string = resourceGroup().location

@description('Static Web Apps is not in every region. eastasia is the closest supported to India.')
param swaLocation string = 'eastasia'

@description('Entra object id of the GitHub OIDC app. Receives Key Vault + ACR rights.')
param deployerPrincipalId string

@description('Public storefront origins allowed by gateway CORS (comma-separated).')
param webOrigins string = 'http://localhost:5173,https://eco-organic-store.com,https://www.eco-organic-store.com'

var suffix = uniqueString(resourceGroup().id)
var acrName = take('acrharvest${suffix}', 50)
var kvName = take('kvh${suffix}', 24)
var cosmosName = take('cosmos-harvest-${suffix}', 44)
var caeName = 'cae-harvest'
var lawName = 'law-harvest'
var identityName = 'id-harvest-apps'
var swaName = 'swa-harvest'
var databaseNames = [
  'identity'
  'catalog'
  'cart'
  'inventory'
  'order'
  'payment'
  'review'
]

resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: identityName
  location: location
}

resource law 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: lawName
  location: location
  properties: {
    sku: { name: 'PerGB2018' }
    retentionInDays: 30
  }
}

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: { name: 'Basic' }
  properties: {
    adminUserEnabled: false
    publicNetworkAccess: 'Enabled'
  }
}

resource cosmos 'Microsoft.DocumentDB/databaseAccounts@2024-05-15' = {
  name: cosmosName
  location: location
  kind: 'MongoDB'
  properties: {
    databaseAccountOfferType: 'Standard'
    publicNetworkAccess: 'Enabled'
    disableLocalAuth: false
    consistencyPolicy: {
      defaultConsistencyLevel: 'Session'
    }
    locations: [
      {
        locationName: location
        failoverPriority: 0
        isZoneRedundant: false
      }
    ]
    capabilities: [
      { name: 'EnableMongo' }
      { name: 'EnableServerless' }
    ]
    apiProperties: {
      serverVersion: '7.0'
    }
  }
}

resource databases 'Microsoft.DocumentDB/databaseAccounts/mongodbDatabases@2024-05-15' = [for db in databaseNames: {
  parent: cosmos
  name: db
  properties: {
    resource: {
      id: db
    }
  }
}]

resource kv 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: kvName
  location: location
  properties: {
    tenantId: subscription().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: false
    enableSoftDelete: true
    enablePurgeProtection: true
    publicNetworkAccess: 'Enabled'
    accessPolicies: [
      {
        tenantId: subscription().tenantId
        objectId: deployerPrincipalId
        permissions: {
          secrets: ['get', 'list', 'set', 'delete']
        }
      }
      {
        tenantId: subscription().tenantId
        objectId: identity.properties.principalId
        permissions: {
          secrets: ['get', 'list']
        }
      }
    ]
  }
}

var mongoConnection = cosmos.listConnectionStrings().connectionStrings[0].connectionString

resource mongoSecrets 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = [for db in databaseNames: {
  parent: kv
  name: 'cosmos-${db}-uri'
  properties: {
    value: replace(mongoConnection, '/?', '/${db}?')
  }
}]

resource cae 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: caeName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: law.properties.customerId
        sharedKey: law.listKeys().primarySharedKey
      }
    }
  }
}

resource swa 'Microsoft.Web/staticSites@2022-03-01' = {
  name: swaName
  location: swaLocation
  sku: {
    name: 'Free'
    tier: 'Free'
  }
  properties: {
    provider: 'None'
  }
}

var acrPullRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
var acrPushRole = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '8311e382-0749-4cb8-b61a-304f252e45ec')

resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, identity.id, acrPullRole)
  scope: acr
  properties: {
    roleDefinitionId: acrPullRole
    principalId: identity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

resource acrPush 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, deployerPrincipalId, acrPushRole)
  scope: acr
  properties: {
    roleDefinitionId: acrPushRole
    principalId: deployerPrincipalId
    principalType: 'ServicePrincipal'
  }
}

output acrName string = acr.name
output acrLoginServer string = acr.properties.loginServer
output environmentName string = cae.name
output environmentId string = cae.id
output identityName string = identity.name
output identityId string = identity.id
output keyVaultName string = kv.name
output keyVaultUri string = kv.properties.vaultUri
output swaName string = swa.name
output swaDefaultHostname string = swa.properties.defaultHostname
output cosmosName string = cosmos.name
output webOrigins string = webOrigins
output location string = location
