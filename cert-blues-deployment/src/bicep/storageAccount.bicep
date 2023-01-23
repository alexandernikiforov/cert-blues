@description('The name of the storage account')
param name string

@description('The location of the storage account resource')
param location string

@description('SKU of the storage account, default is Standard_LRS')
param sku string = 'Standard_LRS'

@description('The tags used for this deployment')
param tags object = {}

@description('The name of the table storing certificate requests')
param tableName string = 'requests'

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
    minimumTlsVersion: 'TLS1_2'
  }

  tags: tags

  // table storage
  resource tables 'tableServices' = {
    name: 'default'

    // request table
    resource requestQueue 'tables' = {
      name: tableName
    }
  }
  
}

