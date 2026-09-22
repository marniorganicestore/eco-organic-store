@description('Container App name (also the in-environment DNS name).')
param appName string

param location string
param environmentId string
param containerImage string
param userAssignedIdentityId string
param acrLoginServer string
param keyVaultUri string

@description('True only for the public gateway.')
param externalIngress bool = false

param minReplicas int = 0
param maxReplicas int = 3

@description('Key Vault secret names to mount as Container App secrets (same name).')
param keyVaultSecretNames array = []

@description('Environment entries: { name, value } and/or { name, secretRef }.')
param envVars array = []

resource app 'Microsoft.App/containerApps@2024-03-01' = {
  name: appName
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${userAssignedIdentityId}': {}
    }
  }
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: externalIngress
        targetPort: 8080
        transport: 'http'
        allowInsecure: false
      }
      registries: [
        {
          server: acrLoginServer
          identity: userAssignedIdentityId
        }
      ]
      secrets: [for secretName in keyVaultSecretNames: {
        name: secretName
        keyVaultUrl: '${keyVaultUri}secrets/${secretName}'
        identity: userAssignedIdentityId
      }]
    }
    template: {
      containers: [
        {
          name: appName
          image: containerImage
          env: union(
            [
              { name: 'PORT', value: '8080' }
              { name: 'JAVA_TOOL_OPTIONS', value: '-XX:MaxRAMPercentage=75.0' }
            ],
            envVars
          )
          resources: {
            cpu: json('0.5')
            memory: '1Gi'
          }
          probes: [
            {
              type: 'Startup'
              tcpSocket: { port: 8080 }
              periodSeconds: 5
              failureThreshold: 30
            }
            {
              type: 'Liveness'
              tcpSocket: { port: 8080 }
              periodSeconds: 15
            }
            {
              type: 'Readiness'
              tcpSocket: { port: 8080 }
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'http-scale'
            http: {
              metadata: {
                concurrentRequests: '20'
              }
            }
          }
        ]
      }
    }
  }
}

output fqdn string = app.properties.configuration.ingress.fqdn
output name string = app.name
