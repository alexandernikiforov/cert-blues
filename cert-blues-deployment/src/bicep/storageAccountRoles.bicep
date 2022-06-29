@description('The name of the storage account')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource storageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' existing = {
  name: name
}

// roles for service principal

// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var storageQueueDataContributorId = '974c5e8b-45b9-4653-ba55-5f855dd0fb88' // storage queue data contributor 

// storage queue data contributor to read certificate requests from the queue
var storageQueueDataContributorRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', storageQueueDataContributorId)
resource storageQueueDataContributorRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: storageAccount
  name: guid(storageAccount.id, principalId, storageQueueDataContributorId)
  properties: {
    principalId: principalId
    roleDefinitionId: storageQueueDataContributorRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}
