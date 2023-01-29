@description('The name of the VNET')
param name string

@description('The location of the VNET')
param location string

@description('The tags used for this deployment')
param tags object = {}

resource vnet 'Microsoft.Network/virtualNetworks@2022-07-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.40.0.0/20'
      ]
    }

    subnets: [
      {
        name: 'aci-subnet'
        properties: {
          addressPrefix: '10.40.0.0/24'
          // this a subnet for ACI only
          delegations: [
            {
              name: 'Microsoft.ContainerInstance.containerGroups'
              properties: {
                serviceName: 'Microsoft.ContainerInstance/containerGroups'
              }
            }
          ]
          serviceEndpoints: [
            {
              locations: [
                location
              ]
              service: 'Microsoft.Storage'
            }
            {
              locations: [
                location
              ]
              service: 'Microsoft.KeyVault'
            }
          ]
        }
      }
    ]
  }
}

output vnetId string = vnet.id
output subnetId string = vnet.properties.subnets[0].id
