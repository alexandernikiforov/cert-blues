@description('The name of the container group to run container with the cert bot, must be unique in Azure')
param name string

@description('The location of the storage account resource')
param location string

@description('The full name of the Docker image to pull to create the running container')
param appImage string

resource containerGroup 'Microsoft.ContainerInstance/containerGroups@2021-10-01' = {
  name: name
  location: location
  // assign the system identity to this container group
  identity: {
    type: 'SystemAssigned'
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
        }
      }
    ]
    osType: 'Linux'
    restartPolicy: 'Never'
  }
}

output managedIdentityId string = containerGroup.identity.principalId
