@description('The name of the key vault')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

@description('ID of the tenant where the principal is residing, default is the tenant of the current subscription')
param tenantId string = subscription().tenantId

// Object ID of the Azure CDN service principal
var azureCdnObjectId = '8e28b74f-37d7-4ab9-bbc7-7af6c6e7cc2d'

resource keyVault 'Microsoft.KeyVault/vaults@2021-06-01-preview' existing = {
  name: name
}

resource accessPolicies 'Microsoft.KeyVault/vaults/accessPolicies@2019-09-01' = {
  name: 'add'
  parent: keyVault
  properties: {
    accessPolicies: [
      {
        // permissions for Azure CDN
        objectId: azureCdnObjectId
        tenantId: tenantId
        permissions: {
          secrets: [
            'get'
          ]
          certificates: [
            'get'
          ]
        }
      }
      {
        // permissions for the application that will create certificates in this key vault
        objectId: principalId
        tenantId: tenantId
        permissions: {
          keys: [
            'all'
          ]
          certificates: [
            'all'
          ]
          secrets: [
            'all'
          ]
        }
      }
    ]
  }
}
