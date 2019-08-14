# tranSMART 17.2-SNAPSHOT Installation

Below are the installation instructions for tranSMART version 17.2-SNAPSHOT. If you already have an older version of tranSMART, follow the [upgrade guide](upgrade.md) instead.

  1. [Prerequisites](#1-prerequisites)
  2. [Setup database](#2-setup-database)
  3. [Setup configuration](#3-setup-configuration)
  4. [Build and run tranSMART API Server](#4-build-and-run-transmart-api-server)


## 1. Prerequisites

Supported operating systems (some others are likely supported, but not tested):
* Ubuntu 16.04 and 18.04
* CentOS 7
* MacOS Sierra

The module expects certain software and libraries to be present on the system.
They are listed below, the versions mentioned are known to work but others might
as well.

* For all components:
    * JDK 8
* For building tranSMART from sources:
    * [gradle](https://gradle.org). Any version `> 2.12` and `<= 3.5.1` should suffice (version `3.5.1` is recommended). Other versions may cause some build issues.


## 2. Setup database

Database definitions and installation instructions are in [transmart-schemas](../transmart-schemas). 

The database can be created at application startup using Liquibase, by adding this line to the configuration:
```yaml
grails.plugin.databasemigration.updateOnStart: true
```
 
Data can be loaded using [transmart-copy](../transmart-copy), which writes prepared tabular data into the tranSMART database.


## 3. Setup configuration

**tranSMART API Server** is configured in the [application.yml file](../transmart-api-server/grails-app/conf/application.yml). The settings, especially for the database connection and [Keycloak](https://www.keycloak.org/) identity provider, should be overwritten by an external file. See the [transmart-api-server documentation](../transmart-api-server#configure-transmart-to-accept-tokens-from-keycloak) on how to create and use the external file.
Setting it up Keycloak requires [just a few steps](../transmart-api-server#how-to-set-up-authentication-for-the-api-server).

An example configuration file:
```yaml
# Database configuration
dataSource:
    driverClassName: org.postgresql.Driver
    dialect: org.hibernate.dialect.PostgreSQLDialect
    url: jdbc:postgresql://localhost:5432/transmart?currentSchema=public

# Create or update the database schema at application startup 
grails.plugin.databasemigration.updateOnStart: true

# Disable saving application logs in the database 
org.transmartproject.system.writeLogToDatabase: false

# By default, users without any role are not denied access
org.transmartproject.security.denyAccessToUsersWithoutRole: false

# Keycloak configuration
keycloak:
    realm: transmart-dev
    bearer-only: true
    auth-server-url: https://keycloak-dwh-test.thehyve.net/auth
    resource: transmart-client
    use-resource-role-mappings: true
```


## 4. Build and run tranSMART API Server

### From source code

The project is built using [gradle](https://gradle.org).
To build the project, run:
```
gradle :transmart-api-server:assemble
```
This should create the file `transmart-api-server/build/libs/transmart-api-server-17.2-SNAPSHOT.war`.
Run it in production mode with:
```
java -jar -Dspring.config.location=/path/to/config.yaml transmart-api-server/build/libs/transmart-api-server-17.2-SNAPSHOT.war
```
Or in development mode with:
```
cd transmart-api-server
grails run-app -Dspring.config.location=/path/to/config.yaml
```

### From a Nexus repository

Deployment artefacts are published to [the Nexus repository of The Hyve](https://repo.thehyve.nl/).

To fetch and run `transmart-api-server`:
```bash
# Fetch artefacts from Maven
TRANSMART_VERSION=17.2-SNAPSHOT
curl -f -L https://repo.thehyve.nl/service/local/repositories/releases/content/org/transmartproject/transmart-api-server/${TRANSMART_VERSION}/transmart-api-server-${TRANSMART_VERSION}.war -o transmart-api-server-${TRANSMART_VERSION}.war && \
# Run it with:
java -jar -Dspring.config.location=/path/to/config.yaml transmart-api-server-${TRANSMART_VERSION}.war
```
