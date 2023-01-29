@description('The name of the storage account')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource storageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' existing = {
  name: name
}

// roles for service principal

// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var storageBlobDataContributorId = 'ba92f5b4-2d11-453d-a403-e96b0029c9fe' // storage blob data contributor 

// storage blob data contributor to write the HTTP challenge tokens to the storage
var storageBlobDataContributorRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', storageBlobDataContributorId)
resource storageBlobDataContributorRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: storageAccount
  name: guid(storageAccount.id, principalId, storageBlobDataContributorId)
  properties: {
    principalId: principalId
    roleDefinitionId: storageBlobDataContributorRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}
