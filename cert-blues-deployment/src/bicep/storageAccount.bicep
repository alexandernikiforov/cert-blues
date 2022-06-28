@description('The name of the storage account')
param name string

@description('The location of the storage account resource')
param location string

@description('SKU of the storage account, default is Standard_LRS')
param sku string = 'Standard_LRS'

@description('The tags used for this deployment')
param tags object = {}

@description('The name of the queue storing certificate requests')
param queueName string = 'requests'

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource storageAccount 'Microsoft.Storage/storageAccounts@2021-06-01' = {
  name: name
  location: location
  sku: {
    name: sku
  }
  kind: 'StorageV2'
  properties: {
    accessTier: 'Hot'
    allowBlobPublicAccess: false
  }

  tags: tags

  // queues
  resource queues 'queueServices' = {
    name: 'default'

    // request queue
    resource requestQueue 'queues' = {
      name: queueName
    }
  }
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
