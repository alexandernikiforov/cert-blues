param location string = resourceGroup().location

var tags = {
  application: 'cert-blues'
}

var seed = resourceGroup().id

var keyVaultName = 'cert-blues-${uniqueString(seed)}'
var storageAccountName = 'certblues${uniqueString(seed)}'
var identityName = 'cert-blues-${uniqueString(seed)}'
var accountKeyName = 'account-key'
var vnetName = resourceGroup().name

module identityModule 'identity.bicep' = {
  name: 'certBluesIdentity'
  params: {
    name: identityName
    location: location
  }
}

// define the network with a subnet for the ACI to be deployed into
module networkModule 'network.bicep' = {
  name: 'vnet'
  params: {
    location: location
    name: vnetName
    tags: tags
  }
}

// access to the key vault and the storage account is restricted to the just created subnet
module keyVaultModule 'keyVault.bicep' = {
  name: 'keyVault'
  params: {
    name: keyVaultName
    accountKeyName: accountKeyName
    location: location
    tags: tags
  }
}

// access to the key vault and the storage account is restricted to the just created subnet
module storageAccountModule 'storageAccount.bicep' = {
  name: 'storageAccount'
  params: {
    name: storageAccountName
    location: location
    tags: tags
  }
}

// assign roles for the container group, read the ID of the managed identity
var principalId = identityModule.outputs.managedIdentityId

module roleAssignmentModule 'roleAssignment.bicep' = {
  name: 'roleAssignment'
  dependsOn: [
    storageAccountModule
    keyVaultModule
  ]
  params: {
    storageAccountName: storageAccountName
    keyVaultName: keyVaultName
    principalId: principalId
  }
}

