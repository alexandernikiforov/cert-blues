@description('The name of the key vault')
param name string

@description('The location of the key vault resource')
param location string

@description('The tags used for this deployment')
param tags object = {}

// @description('Which subnets are allowed to access this key vault')
// param allowedSubnetIds array

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
    Validity of the account key as duration, default it 6 month (P6M)
    See https://en.wikipedia.org/wiki/ISO_8601#Durations
  ''')
param accountKeyValidity string = 'P6M'

// @description('Expiration time of the account key')
// param accountKeyExpiration int = dateTimeToEpoch(dateTimeAdd(utcNow(), accountKeyValidity))

resource keyVault 'Microsoft.KeyVault/vaults@2022-07-01' = {
  location: location
  name: name
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    publicNetworkAccess: 'Enabled'
    // networkAcls: {
    //   bypass: 'AzureServices'
    //   defaultAction: 'Deny'
    //   virtualNetworkRules: [for allowedSubnetId in allowedSubnetIds: {
    //     ignoreMissingVnetServiceEndpoint: false
    //     id: allowedSubnetId
    //   }]
    // }
    tenantId: subscription().tenantId
  }
  tags: tags

  // account key
  resource accountKey 'keys@2022-07-01' = {
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
        // rotate 30 days before expiry
        lifetimeActions: [
          {
            action: {
              type: 'rotate'
            }
            trigger: {
              timeBeforeExpiry: 'P30D'
            }
          }
          {
            action: {
              type: 'notify'
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

output keyVaultUri string = keyVault.properties.vaultUri
output keyVaultId string = keyVault.id
