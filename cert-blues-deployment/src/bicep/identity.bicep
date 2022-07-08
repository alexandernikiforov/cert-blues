@description('The name of user-assigned identity to create')
param name string

@description('The location of the identity resource')
param location string

@description('The tags used for this deployment')
param tags object = {}

resource certBluesIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2022-01-31-preview' = {
  name: name
  location: location
  tags: tags
}

output managedIdentityId string = certBluesIdentity.properties.principalId
