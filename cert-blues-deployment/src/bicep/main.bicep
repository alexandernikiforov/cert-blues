param location string = resourceGroup().location

var tags = {
  environment: 'dev'
}

var seed = resourceGroup().id
// var keyVaultName = 'cert-blues-dev-${uniqueString(seed)}'
// var storageAccountName = 'certblues${uniqueString(seed)}'

var keyVaultName = 'cert-blues-dev'
var accountKeyName = 'account-key-${uniqueString(seed)}'
var storageAccountName = 'certbluesdev'
var identityName = 'cert-blues-dev'

module identityModule 'identity.bicep' = {
  name: 'certBluesIdentity'
  params: {
    name: identityName
    location: location
  }
}

module keyVaultModule 'keyVault.bicep' = {
  name: 'keyVault'
  params: {
    name: keyVaultName
    accountKeyName: accountKeyName
    location: location
    tags: tags
  }
}

module storageAccountModule 'storageAccount.bicep' = {
  name: 'storageAccount'
  params: {
    name: storageAccountName
    location: location
    tags: tags
  }
}

// roles for the container group, read the ID of the managed identity
var principalId = identityModule.outputs.managedIdentityId

module dnsZoneRolesModule 'dnsZone.bicep' = {
  name: 'dnsZoneRoles'
  scope: resourceGroup('mydomainnames')
  params: {
    name: 'cloudalni.com'
    principalId: principalId
  }
}

module storageAccountRolesModule 'storageAccountRoles.bicep' = {
  name: 'storageAccountRoles'
  dependsOn: [
    storageAccountModule
  ]
  params: {
    name: storageAccountName
    principalId: principalId
  }
}

module keyVaultRolesModule 'keyVaultRoles.bicep' = {
  name: 'keyVaultRoles'
  dependsOn: [
    keyVaultModule
  ]
  params: {
    name: keyVaultName
    principalId: principalId
  }
}

// add roles to the test application, we should be able to use it outside of the container group
// object ID of the application registration for 'cert-blues-dev'
var certBluesDevAppId = '36ffc98b-398b-4558-9009-f7e4922cdf97'

module dnsZoneRolesForTestModule 'dnsZone.bicep' = {
  name: 'dnsZoneRolesForTest'
  scope: resourceGroup('mydomainnames')
  params: {
    name: 'cloudalni.com'
    principalId: certBluesDevAppId
  }
}

module storageAccountRolesForTestModule 'storageAccountRoles.bicep' = {
  name: 'storageAccountRolesForTest'
  dependsOn: [
    storageAccountModule
  ]
  params: {
    name: storageAccountName
    principalId: certBluesDevAppId
  }
}

module keyVaultRolesForTestModule 'keyVaultRoles.bicep' = {
  name: 'keyVaultRolesForTest'
  dependsOn: [
    keyVaultModule
  ]
  params: {
    name: keyVaultName
    principalId: certBluesDevAppId
  }
}
