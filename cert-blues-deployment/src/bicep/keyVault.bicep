@description('The name of the key vault')
param name string

@description('The location of the key vault resource')
param location string

@description('The tags used for this deployment')
param tags object = {}

@description('The name of the account key')
param accountKeyName string = 'accountKey'

@description('The size of the account key, default is 2048')
@allowed([
  2048
  3072
  4096
])
param accountKeySize int = 2048

@description('Key algorithm, default is RSA')
@allowed([
  'RSA'
  'EC'
])
param accountKeyAlg string = 'RSA'

@description(''' 
    Validity of the account key as duration, default it 3 month (P3M)
    See https://en.wikipedia.org/wiki/ISO_8601#Durations
  ''')
param accountKeyValidity string = 'P3M'

// @description('Expiration time of the account key')
// param accountKeyExpiration int = dateTimeToEpoch(dateTimeAdd(utcNow(), accountKeyValidity))

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource keyVault 'Microsoft.KeyVault/vaults@2021-06-01-preview' = {
  location: location
  name: name
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    accessPolicies: []
    tenantId: subscription().tenantId
  }
  tags: tags

  // account key
  resource accountKey 'keys@2021-06-01-preview' = {
    name: accountKeyName
    properties: {
      keySize: accountKeySize
      kty: accountKeyAlg
      keyOps: [
        'sign'
        'verify'
        'decrypt'
        'encrypt'
      ]
      rotationPolicy: {
        attributes: {
          expiryTime: accountKeyValidity
        }
        // notify 30 days before expiry
        lifetimeActions: [
          {
            action: {
              type: 'rotate'
            }
            trigger: {
              timeBeforeExpiry: 'P30D'
            }
          }
        ]
      }
      attributes: {
        enabled: true
      }
    }
  }
}

// roles for service principal

// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var cryptoUserRoleId = '14b46e9e-c2b7-41b4-b07b-48a6ebf60603' // key vault crypto user 
var certificatesOfficerRoleId = 'a4417e6f-fecd-4de8-b567-7b0420556985' // key vault certificates officer 

// crypto user to perform operations with the account key
var cryptoUserRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', cryptoUserRoleId)
resource cryptoUserRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: keyVault
  name: guid(keyVault.id, principalId, cryptoUserRoleId)
  properties: {
    principalId: principalId
    roleDefinitionId: cryptoUserRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}

// certificates officer to create and change certificates
var certificatesOfficerRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', certificatesOfficerRoleId)
resource certificatesOfficerRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: keyVault
  name: guid(keyVault.id, principalId, certificatesOfficerRoleId)
  properties: {
    principalId: principalId
    roleDefinitionId: certificatesOfficerRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}

output keyVaultUri string = keyVault.properties.vaultUri

