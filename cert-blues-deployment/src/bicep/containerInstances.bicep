@description('The name of the container group to run container with the cert bot, must be unique in Azure')
param name string

@description('The location of the container image resource')
param location string

@description('The name of the user-managed identity to assign to this container group')
param identity string

@description('The name virtual network this container group should be deployed into')
param vnetName string = resourceGroup().name

@description('The name of the user-managed identity to assign to this container group')
param subnetName string = 'aci-subnet'

@description('Array of key-value pairs to be set up into the container\'s environment')
param environment array

@description('The full name of the Docker image to pull to create the running container')
param appImage string

@description('ID of the log analytics workspace')
param logAnalyticsWorkspaceId string

@description('Key of the log analytics workspace')
@secure()
param logAnalyticsWorkspaceKey string

// existing user-assigned identity will be assigned to the container group
resource certBluesIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2022-01-31-preview' existing = {
  name: identity
}

// subnet this container group should be deployed into
resource vnet 'Microsoft.Network/virtualNetworks@2022-07-01' existing = {
  name: vnetName
}

resource subnet 'Microsoft.Network/virtualNetworks/subnets@2022-07-01' existing = {
  parent: vnet
  name: subnetName
}

resource containerGroup 'Microsoft.ContainerInstance/containerGroups@2022-09-01' = {
  name: name
  location: location
  // assign the user-managed identity to this container group
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${certBluesIdentity.id}': {}
    }
  }
  properties: {
    subnetIds: [
      {
        id: subnet.id
      }
    ]
    containers: [
      {
        name: 'cert-blues-app'
        properties: {
          image: appImage
          resources: {
            requests: {
              cpu: 1
              memoryInGB: 1
            }
          }
          // these environment variables are needed to manage the DNS zone
          environmentVariables: environment
        }
      }
    ]
    diagnostics: {
      logAnalytics:{
        logType: 'ContainerInsights'
        workspaceId: logAnalyticsWorkspaceId
        workspaceKey: logAnalyticsWorkspaceKey
      }
    }
    osType: 'Linux'
    restartPolicy: 'Never'
  }
}
