@description('The name of the storage account')
param storageAccountName string

@description('The name of the key vault')
param keyVaultName string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

// resources which will be accessed by the application
resource storageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' existing = {
  name: storageAccountName
}

resource keyVault 'Microsoft.KeyVault/vaults@2021-06-01-preview' existing = {
  name: keyVaultName
}

// roles for service principal
// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var storageTableDataContributorId = '0a9a7e1f-b9d0-4cc4-a60d-0319b160aaa3' // storage table data contributor 
var cryptoUserRoleId = '14b46e9e-c2b7-41b4-b07b-48a6ebf60603' // key vault crypto user 

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

