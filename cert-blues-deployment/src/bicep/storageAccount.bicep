@description('The name of the storage account')
param name string

@description('The location of the storage account resource')
param location string

@description('SKU of the storage account, default is Standard_LRS')
param sku string = 'Standard_LRS'

// @description('Which subnets are allowed to access this key vault')
// param allowedSubnetIds array = []

@description('The tags used for this deployment')
param tags object = {}

@description('The name of the table storing certificate requests')
param tableName string = 'requests'

resource storageAccount 'Microsoft.Storage/storageAccounts@2022-09-01' = {
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
    publicNetworkAccess: 'Enabled'
    // networkAcls: {
    //   bypass: 'AzureServices'
    //   defaultAction: 'Deny'
    //   virtualNetworkRules: [for allowedSubnetId in allowedSubnetIds: {
    //     id: allowedSubnetId
    //   }]
    // }
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

output storageAccountId string = storageAccount.id
