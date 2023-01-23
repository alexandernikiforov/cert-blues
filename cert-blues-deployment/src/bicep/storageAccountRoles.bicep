@description('The name of the storage account')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource storageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' existing = {
  name: name
}

// roles for service principal

// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var storageTableDataContributorId = '0a9a7e1f-b9d0-4cc4-a60d-0319b160aaa3' // storage table data contributor 

// storage table data contributor to read certificate requests from the table
var storageTableDataContributorRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', storageTableDataContributorId)
resource storageTableDataContributorRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: storageAccount
  name: guid(storageAccount.id, principalId, storageTableDataContributorId)
  properties: {
    principalId: principalId
    roleDefinitionId: storageTableDataContributorRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}
