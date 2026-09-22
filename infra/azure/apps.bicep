param location string
param imageTag string
param acrName string
param environmentName string
param identityName string
param keyVaultName string

@description('Public storefront origin used for Stripe redirects, e.g. https://eco-organic-store.com')
param storefrontUrl string

@description('Gateway CORS allow-list (comma-separated origins).')
param webOrigins string

param gatewayMinReplicas int = 0

var acrLoginServer = acr.properties.loginServer
var environmentId = cae.id
var identityId = identity.id
var keyVaultUri = kv.properties.vaultUri

var identityUrl = 'http://identity-service'
var catalogUrl = 'http://catalog-service'
var cartUrl = 'http://cart-service'
var inventoryUrl = 'http://inventory-service'
var orderUrl = 'http://order-service'
var paymentUrl = 'http://payment-service'
var reviewUrl = 'http://review-service'

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = {
  name: acrName
}

resource cae 'Microsoft.App/managedEnvironments@2024-03-01' existing = {
  name: environmentName
}

resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' existing = {
  name: identityName
}

resource kv 'Microsoft.KeyVault/vaults@2023-07-01' existing = {
  name: keyVaultName
}

module gateway 'modules/container-app.bicep' = {
  name: 'app-gateway'
  params: {
    appName: 'gateway'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/gateway:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    externalIngress: true
    minReplicas: gatewayMinReplicas
    keyVaultSecretNames: [
      'jwt-secret'
    ]
    envVars: [
      { name: 'JWT_SECRET', secretRef: 'jwt-secret' }
      { name: 'CORS_ALLOWED_ORIGINS', value: webOrigins }
      { name: 'IDENTITY_SERVICE_URL', value: identityUrl }
      { name: 'CATALOG_SERVICE_URL', value: catalogUrl }
      { name: 'CART_SERVICE_URL', value: cartUrl }
      { name: 'INVENTORY_SERVICE_URL', value: inventoryUrl }
      { name: 'ORDER_SERVICE_URL', value: orderUrl }
      { name: 'PAYMENT_SERVICE_URL', value: paymentUrl }
      { name: 'REVIEW_SERVICE_URL', value: reviewUrl }
    ]
  }
}

module identityService 'modules/container-app.bicep' = {
  name: 'app-identity'
  params: {
    appName: 'identity-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/identity-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'jwt-secret'
      'internal-api-key'
      'cosmos-identity-uri'
      'google-client-id'
      'admin-password'
    ]
    envVars: [
      { name: 'JWT_SECRET', secretRef: 'jwt-secret' }
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-identity-uri' }
      { name: 'GOOGLE_CLIENT_ID', secretRef: 'google-client-id' }
      { name: 'ADMIN_PASSWORD', secretRef: 'admin-password' }
      { name: 'COOKIE_SECURE', value: 'true' }
      { name: 'COOKIE_SAME_SITE', value: 'None' }
    ]
  }
}

module catalogService 'modules/container-app.bicep' = {
  name: 'app-catalog'
  params: {
    appName: 'catalog-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/catalog-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'internal-api-key'
      'cosmos-catalog-uri'
    ]
    envVars: [
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-catalog-uri' }
    ]
  }
}

module cartService 'modules/container-app.bicep' = {
  name: 'app-cart'
  params: {
    appName: 'cart-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/cart-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'internal-api-key'
      'cosmos-cart-uri'
    ]
    envVars: [
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-cart-uri' }
      { name: 'CATALOG_SERVICE_URL', value: catalogUrl }
      { name: 'INVENTORY_SERVICE_URL', value: inventoryUrl }
    ]
  }
}

module inventoryService 'modules/container-app.bicep' = {
  name: 'app-inventory'
  params: {
    appName: 'inventory-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/inventory-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'internal-api-key'
      'cosmos-inventory-uri'
    ]
    envVars: [
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-inventory-uri' }
    ]
  }
}

module orderService 'modules/container-app.bicep' = {
  name: 'app-order'
  params: {
    appName: 'order-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/order-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'internal-api-key'
      'cosmos-order-uri'
    ]
    envVars: [
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-order-uri' }
      { name: 'CART_SERVICE_URL', value: cartUrl }
      { name: 'INVENTORY_SERVICE_URL', value: inventoryUrl }
      { name: 'CATALOG_SERVICE_URL', value: catalogUrl }
      { name: 'PAYMENT_SERVICE_URL', value: paymentUrl }
    ]
  }
}

module paymentService 'modules/container-app.bicep' = {
  name: 'app-payment'
  params: {
    appName: 'payment-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/payment-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'internal-api-key'
      'cosmos-payment-uri'
      'stripe-secret-key'
      'stripe-webhook-secret'
    ]
    envVars: [
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-payment-uri' }
      { name: 'ORDER_SERVICE_URL', value: orderUrl }
      { name: 'STRIPE_SECRET_KEY', secretRef: 'stripe-secret-key' }
      { name: 'STRIPE_WEBHOOK_SECRET', secretRef: 'stripe-webhook-secret' }
      { name: 'STRIPE_SUCCESS_URL', value: '${storefrontUrl}/order/success?session_id={CHECKOUT_SESSION_ID}' }
      { name: 'STRIPE_CANCEL_URL', value: '${storefrontUrl}/cart' }
    ]
  }
}

module reviewService 'modules/container-app.bicep' = {
  name: 'app-review'
  params: {
    appName: 'review-service'
    location: location
    environmentId: environmentId
    containerImage: '${acrLoginServer}/review-service:${imageTag}'
    userAssignedIdentityId: identityId
    acrLoginServer: acrLoginServer
    keyVaultUri: keyVaultUri
    keyVaultSecretNames: [
      'internal-api-key'
      'cosmos-review-uri'
    ]
    envVars: [
      { name: 'INTERNAL_API_KEY', secretRef: 'internal-api-key' }
      { name: 'MONGODB_URI', secretRef: 'cosmos-review-uri' }
      { name: 'ORDER_SERVICE_URL', value: orderUrl }
      { name: 'CATALOG_SERVICE_URL', value: catalogUrl }
    ]
  }
}

output gatewayFqdn string = gateway.outputs.fqdn
output gatewayUrl string = 'https://${gateway.outputs.fqdn}'
