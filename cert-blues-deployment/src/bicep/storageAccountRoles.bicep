@description('The name of the storage account')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource storageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' existing = {
  name: name
}

