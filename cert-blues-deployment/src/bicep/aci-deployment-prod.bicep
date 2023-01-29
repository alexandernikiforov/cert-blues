@description('Location of the container group resource')
param location string = resourceGroup().location

@description('ID of the subscription, it is used to manage the DNS zone in the container')
param subscriptionId string = subscription().subscriptionId

@description('ID of the tenant, it is used to manage the DNS zone in the container')
param tenantId string = subscription().tenantId

@description('Docker image to deploy to the container group')
param image string

@description('ID of the log analytics workspace')
param logAnalyticsWorkspaceId string

@description('Key of the log analytics workspace')
@secure()
param logAnalyticsWorkspaceKey string

var seed = resourceGroup().id

var containerGroupName = 'cert-blues-prod-${uniqueString(seed)}'
var identity = 'cert-blues-${uniqueString(seed)}'

module containerGroupModule 'containerInstances.bicep' = {
  name: 'containers'
  params: {
    name: containerGroupName
    identity: identity
    location: location
    logAnalyticsWorkspaceId: logAnalyticsWorkspaceId
    logAnalyticsWorkspaceKey: logAnalyticsWorkspaceKey
    environment: [
      {
        name: 'SPRING_PROFILES_ACTIVE'
        value: 'prod'
      }
      {
        name: 'AZURE_SUBSCRIPTION_ID'
        value: subscriptionId
      }
      {
        name: 'AZURE_TENANT_ID'
        value: tenantId
      }
    ]
    appImage: image
  }
}

