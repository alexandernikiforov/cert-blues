@description('The name of the container group to run container with the cert bot, must be unique in Azure')
param name string

@description('The name of the user-managed identity to assign to this container group')
param identity string

@description('The location of the storage account resource')
param location string

@description('The location of the storage account resource')
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

resource containerGroup 'Microsoft.ContainerInstance/containerGroups@2021-10-01' = {
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
