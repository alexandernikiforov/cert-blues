# cert-blues

![Build](https://github.com/alexandernikiforov/cert-blues/actions/workflows/push-workflow.yml/badge.svg?branch=main)

The project aims to create an Azure-based certificate bot that would provide and renew a Let's Encrypt certificate.
Certificates are generated in Azure Key Vaults and thus can be used from there by a number of services including CDN.
Both HTTP and DNS challenges are supported with HTTP challenges taking precedence if configured.

This project may be forked and adapted to existing infrastructure. See below how to do this.


<!-- TOC -->
* [cert-blues](#cert-blues)
* [Development](#development)
  * [Dev Environment](#dev-environment)
    * [Key Vault](#key-vault)
    * [Storage Account / Table Storage](#storage-account--table-storage)
    * [DNS](#dns)
    * [Target Storage Account](#target-storage-account)
    * [Target Key Vault](#target-key-vault)
    * [Environment Variables](#environment-variables)
    * [Integration Tests](#integration-tests)
    * [Staging Tests](#staging-tests)
* [Known Issues](#known-issues)
<!-- TOC -->

# Development

## Dev Environment

The development environment is used for development. It supports running the code from the development machine,
whereas the higher environments are using Azure Container Instances (ACI) to run the code. Azure resources for the
development environments are not deployed via automatic scripts, but have been created manually.

For configuration see `cert-blues-app/src/main/resources/application-dev.yml`.

For development purposes a special application registration is created. This application is used to access the needed
Azure resources in integration and staging tests.

### Key Vault

The link is `https://cert-blues-dev.vault.azure.net/`. The client application should have the following role
assignments:

* Key Vault Certificates Officer
* Key Vault Crypto Officer

### Storage Account / Table Storage

The link is `https://certbluesdev.table.core.windows.net`. The client application should have the following role
assignments:

* Storage Table Data Contributor

### DNS

The tests run on behalf of the client application should be able to read and write (TXT) entries in the DNS zone
to be able to create DNS challenges. The following role assignments are necessary:

* DNS Zone Contributor

### Target Storage Account

The HTTP challenges are created in Azure Blob Storage with configured static website. The tests run on behalf of the
client application should have the following role assignments (on the $web container level):

* Storage Blob Data Contributor

The link is `https://cloudalnitest.blob.core.windows.net/$web`.

Note that for the HTTP challenges to be able to checked correctly, the HTTP access should be enabled both
for the endpoint's origin in CDN and for the target storage account.

To enable the HTTP access in the endpoint configuration go to `Settings > Origin > Origins (Configure Origin)`.
Make sure both HTTP and HTTPs are allowed.

To enable the HTTP access in the storage account `Overview` and **disable
** `Require secure transfer for REST API operations`
in the `Security` section.

### Target Key Vault

The target key vault holds the provisioned certificate. The link is `https://cert-blues-dev.vault.azure.net`.
It is the same key vault as used for the Let's Encrypt account key.

### Environment Variables

The following environment variables are expected successfully run
the staging test:

| Name                  | Description                                                       | 
|-----------------------|-------------------------------------------------------------------|
| AZURE_CLIENT_ID       | Application (client) ID of the application registration in Entra. |
| AZURE_CLIENT_SECRET   | A valid client secret.                                            |
| AZURE_SUBSCRIPTION_ID | ID of the subscription holding the resources accessed in test.    |
| AZURE_TENANT_ID       | Tenant (directory) ID in Entra.                                   |

The same environment variables are also configured from GitHub actions secrets to run the action for the staging test.
Go to `Settings > Secrets and variables > Actions` to configure the corresponding secrets on GitHub.

### Integration Tests

Integration tests are not supposed to access resource deployed in Azure. The main goal of these tests is to test
the ACME integration. For that purpose Pebble container is used. To
run `cert-blues-certbot/src/integrationTest/java/ch/alni/certblues/certbot/CertBotTest.java`
start the following Docker Compose configuration `cert-blues-acme/docker-compose.yml`.

### Staging Tests

The main staging test that is also run from the GitHub Action workflow
is `cert-blues-app/src/stagingTest/java/ch/alni/certblues/app/AcmeStagingTest.java`.
It is run from the development machine (or from the pipeline) but it actually access resources on Azure .

# Deployment

# Known Issues

ACME DNS challenges are currently not working properly. The challenges are correctly provisioned, but the submitted
to ACME too earlier for the checker to find them in the DNS directory. As a workaround, the HTTP challenges
have the precedence.

A better solution should be found to limit access to the target storage account to CDN only.
