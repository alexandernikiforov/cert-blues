param location string = resourceGroup().location

var tags = {
  environment: 'prod'
}

var seed = resourceGroup().id

var keyVaultName = 'cert-blues-${uniqueString(seed)}'
var storageAccountName = 'certblues${uniqueString(seed)}'

var accountKeyName = 'account-key-${uniqueString(seed)}'
var identityName = 'cert-blues-prod'

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

