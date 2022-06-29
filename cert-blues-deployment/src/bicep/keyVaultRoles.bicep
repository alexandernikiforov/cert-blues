@description('The name of the key vault')
param name string

@description('ID of the principal or the managed identity to assign the roles for access to this resource')
param principalId string

resource keyVault 'Microsoft.KeyVault/vaults@2021-06-01-preview' existing = {
  name: name
}

// roles for service principal

// see https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
var cryptoUserRoleId = '14b46e9e-c2b7-41b4-b07b-48a6ebf60603' // key vault crypto user 
var certificatesOfficerRoleId = 'a4417e6f-fecd-4de8-b567-7b0420556985' // key vault certificates officer 

// crypto user to perform operations with the account key
var cryptoUserRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', cryptoUserRoleId)
resource cryptoUserRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: keyVault
  name: guid(keyVault.id, principalId, cryptoUserRoleId)
  properties: {
    principalId: principalId
    roleDefinitionId: cryptoUserRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}

// certificates officer to create and change certificates
var certificatesOfficerRoleDefinitionId = extensionResourceId(subscription().id, 'Microsoft.Authorization/roleDefinitions', certificatesOfficerRoleId)
resource certificatesOfficerRole 'Microsoft.Authorization/roleAssignments@2020-10-01-preview' = {
  scope: keyVault
  name: guid(keyVault.id, principalId, certificatesOfficerRoleId)
  properties: {
    principalId: principalId
    roleDefinitionId: certificatesOfficerRoleDefinitionId
    principalType: 'ServicePrincipal'
  }
}
