@description('Location of the container group resource')
param location string = resourceGroup().location

@description('ID of the subscription, it is used to manage the DNS zone in the container')
param subscriptionId string = subscription().id

@description('ID of the tenant, it is used to manage the DNS zone in the container')
param tenantId string = subscription().tenantId

@description('Docker image to deploy to the container group')
param image string

var seed = resourceGroup().id

var containerGroupName = 'cert-blues-dev-${uniqueString(seed)}'
var identity = 'cert-blues-dev'

module containerGroupModule 'containerInstances.bicep' = {
  name: 'containers'
  params: {
    name: containerGroupName
    identity: identity
    location: location
    environment: [
      {
        name: 'SPRING_PROFILES_ACTIVE'
        value: 'test'
      }
      {
        name: 'AZURE_SUBSCRIPTION_ID'
        secureValue: subscriptionId
      }
      {
        name: 'AZURE_TENANT_ID'
        secureValue: tenantId
      }
    ]
    appImage: image
  }
}

