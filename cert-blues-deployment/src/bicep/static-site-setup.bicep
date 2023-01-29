@description('Name of the service principal representing the authorized application')
param principalId string

@description('Name of the storage account containing the static web site content')
param storageAccountName string

@description('Name of the key vault that will be holding the certificates for the web site')
param keyVaultName string

@description('The name of the DNS zone')
param dnsZoneName string

@description('The name of resource group of the DNS zone')
param dnsZoneResourceGroupName string

module dnsZoneRolesModule 'dnsZone.bicep' = {
  name: 'dnsZoneRoles'
  scope: resourceGroup(dnsZoneResourceGroupName)
  params: {
    name: dnsZoneName
    principalId: principalId
  }
}

module keyVaultPoliciesModule 'keyVaultPolicies.bicep' = {
  name: 'keyVaultPolicies'
  params: {
    name: keyVaultName
    principalId: principalId
  }
}

module storageAccountRolesModule 'storageAccountRoles.bicep' = {
  name: 'storageAccountRoles'
  params: {
    name: storageAccountName
    principalId: principalId
  }
}
