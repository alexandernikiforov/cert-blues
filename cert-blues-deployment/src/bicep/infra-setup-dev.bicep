// these are the predefined names of the key vault and the storage account that are used for testing in the dev environment
var keyVaultName = 'cert-blues-dev'
var storageAccountName = 'certbluesdev'

// add roles to the dev application, we should be able to use it outside of the container group
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

module roleAssignmentModule 'roleAssignment.bicep' = {
  name: 'roleAssignmentForTest'
  params: {
    storageAccountName: storageAccountName
    keyVaultName: keyVaultName
    principalId: certBluesDevAppId
  }
}

// additionally assign roles to create certificates in the key vault used for the local tests
module keyVaultRolesModule 'keyVaultRoles.bicep' = {
  name: 'keyVaultRolesForTest'
  params: {
    name: keyVaultName
    principalId: certBluesDevAppId
  }
}
