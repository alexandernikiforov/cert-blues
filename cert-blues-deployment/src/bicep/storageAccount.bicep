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

