param location string = resourceGroup().location

var tags = {
  environment: 'dev'
}

var seed = resourceGroup().id
// var keyVaultName = 'cert-blues-dev-${uniqueString(seed)}'
// var storageAccountName = 'certblues${uniqueString(seed)}'

var containerGroupName = 'cert-blues-dev-${uniqueString(seed)}'

var keyVaultName = 'cert-blues-dev'
var accountKeyName = 'account-key-${uniqueString(seed)}'
var storageAccountName = 'certbluesdev'

// object ID of the application registration for 'cert-blues-dev'
// var principalId = '36ffc98b-398b-4558-9009-f7e4922cdf97'

// start by deploying the container group
module containers 'containerInstances.bicep' = {
  name: 'containers'
  params: {
    name: containerGroupName
    location: location
    appImage: 'ghcr.io/alexandernikiforov/cert-blues-app:main'
  }
}

var principalId = containers.outputs.managedIdentityId

module keyVaultModule 'keyVault.bicep' = {
  name: 'keyVault'
  params: {
    name: keyVaultName
    accountKeyName: accountKeyName
    location: location
    tags: tags
    principalId: principalId
  }
}

module storageAccountModule 'storageAccount.bicep' = {
  name: 'storageAccount'
  params: {
    name: storageAccountName
    location: location
    tags: tags
    principalId: principalId
  }
}

module dnsZoneModule 'dnsZone.bicep' = {
  name: 'dnsZone'
  scope: resourceGroup('mydomainnames')
  params: {
    name: 'cloudalni.com'
    principalId: principalId
  }
}

