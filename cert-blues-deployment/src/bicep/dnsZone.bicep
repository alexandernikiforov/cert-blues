@description('The name of the DNS zone to reference in this module')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource dnsZone 'Microsoft.Network/dnsZones@2018-05-01' existing = {
  name: name
}

// roles for service principal

// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var dnsZoneContributorRoleId = 'befefa01-2a29-4197-83a8-272ff33ce314' // DNS Zone Contributor 

// DNS zone contributor to be able to create DNS records in the target zone
var dnsZoneContributorRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', dnsZoneContributorRoleId)
resource dnsZoneContributorContributorRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: dnsZone
  name: guid(dnsZone.id, principalId, dnsZoneContributorRoleId)
  properties: {
    principalId: principalId
    roleDefinitionId: dnsZoneContributorRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}
