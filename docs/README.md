# tranSMART 17.1 Installation

Below are the installation instructions for tranSMART version 17.1. If you already have an older version of tranSMART, follow the [upgrade guide](upgrade.md) instead.

  1. [Prerequisites](#1-prerequisites)
  2. [Setup database](#2-setup-database)
  3. [Setup configuration](#3-setup-configuration)
  4. [Build and run tranSMART Server or tranSMART API Server](#4-build-and-run-transmart-server-or-transmart-api-server)
  5. [Start services](#5-start-services)


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
* For transmart-rserve (names of ubuntu packages, adapt to your OS):
    * libpng12
    * cairo
    * dejavu-sans-fonts
    * dejavu-sans-mono-fonts
    * dejavu-serif-fonts
    * libgfortran
    * libgomp
    * pango
    * readline
    * urw-fonts
    * xorg-x11-fonts-Type1
    * xorg-x11-fonts-misc


## 2. Setup database

Database definitions and installation instructions are in [transmart-data](../transmart-data). 
To set up a test database for tranSMART, you can follow these steps:
* [Drop the database](../transmart-data#drop-the-database)
* [Create the database and load test data](../transmart-data#create-the-database-and-load-test-data)
 
Or to set up an empty tranSMART database follow those:
* [Drop the database](../transmart-data#drop-the-database)
* [Create the database and load the essentials](../transmart-data#create-the-database-and-load-the-essentials)
 
And then a data loading tool based on Spring Batch is available as [transmart-batch](../transmart-batch).


## 3. Setup configuration

**tranSMART API Server** is configured in the [application.yml file](../transmart-api-server/grails-app/conf/application.yml). The settings, especially for the database connection and [Keycloak](https://www.keycloak.org/) identity provider, should be overwritten by an external file. See the [transmart-api-server documentation](../transmart-api-server#configure-transmart-to-accept-tokens-from-keycloak) on how to create and use the external file.
Setting it up Keycloak requires [just a few steps](../transmart-api-server#how-to-set-up-authentication-for-the-api-server).


**tranSMART Server** requires a configuration file to be generated in `~/.grails/transmartConfig`.
Scripts to generate the configuration are shipped with
`transmart-data`.

Either use the sources in this repository (`transmart-core/transmart-data`),
or fetch `transmart-data` from a Nexus repository:
```bash
TRANSMART_VERSION=17.1-HYVE-5.2
curl -f -L https://repo.thehyve.nl/service/local/repositories/releases/content/org/transmartproject/transmart-data/${TRANSMART_VERSION}/transmart-data-${TRANSMART_VERSION}.tar -o transmart-data-${TRANSMART_VERSION}.tar && \
tar xf transmart-data-${TRANSMART_VERSION}.tar 
```
To generate the configuration, please consult the documentation of [transmart-data](../transmart-data).
Once a correct `vars` file has been created, the configuration can be generated and installed
with these commands (requires `php`):
```bash
pushd transmart-data-${TRANSMART_VERSION}
source vars
make -C config install
popd
```


## 4. Build and run tranSMART Server or tranSMART API Server

Instructions for both tranSMART Server and tranSMART API Server are similar. In order to build and run [tranSMART API Server](../transmart-api-server) just replace the `transmart-server` with `transmart-api-server` in the steps below.

### From source code

The project is built using [gradle](https://gradle.org).
To build the project, run:
```
gradle :transmart-server:assemble
```
This should create the file `transmart-server/build/libs/transmart-server-17.1-HYVE-5-SNAPSHOT.war`.
Run it in production mode with:
```
java -jar transmart-server/build/libs/transmart-server-17.1-HYVE-5-SNAPSHOT.war
```
Or in development mode with:
```
cd transmart-server
grails run-app
```
For the API Server, the location of the configuration file needs to be passed on the command line:
```
gradle :transmart-api-server:assemble
java -jar -Dspring.config.location=/path/to/config.yaml transmart-api-server/build/libs/transmart-server-17.1-HYVE-5-SNAPSHOT.war
```


### From a Nexus repository

Deployment artefacts are published to [the Nexus repository of The Hyve](https://repo.thehyve.nl/).

To fetch and run `transmart-server`:
```bash
# Fetch artefacts from Maven
TRANSMART_VERSION=17.1-HYVE-5.2
curl -f -L https://repo.thehyve.nl/service/local/repositories/releases/content/org/transmartproject/transmart-api-server/${TRANSMART_VERSION}/transmart-server-${TRANSMART_VERSION}.war -o transmart-server-${TRANSMART_VERSION}.war && \
# Run it with:
java -jar transmart-server-${TRANSMART_VERSION}.war
```

To fetch and run `transmart-api-server`:
```bash
# Fetch artefacts from Maven
TRANSMART_VERSION=17.1-HYVE-5.2
curl -f -L https://repo.thehyve.nl/service/local/repositories/releases/content/org/transmartproject/transmart-api-server/${TRANSMART_VERSION}/transmart-api-server-${TRANSMART_VERSION}.war -o transmart-api-server-${TRANSMART_VERSION}.war && \
# Run it with:
java -jar -Dspring.config.location=/path/to/config.yaml transmart-api-server-${TRANSMART_VERSION}.war
```


## 5. Start services

### tranSMART Server:

`Rserve` and `Solr` services can be run using `transmart-data`, see [Setup configuration](#3-setup-configuration)
to fetch it from a Nexus repository. 
These instructions are using downloaded binaries, see the documentation of [transmart-data](../transmart-data) if you want 
to use the sources. 
Note that `Rserve` and `Solr` are not necessarily needed for a development installation and 
that they are not used by `transmart-api-server`.

Start `Solr`:
```bash
pushd transmart-data-17.1-HYVE-5.2/solr
java -jar start.jar &
popd
```

`Rserve` can be fetched and installed using `apt` (for `debian` or `ubuntu`) or `yum` (for `redhat` or `centos`).
For `apt`, use:
```bash
# Using apt
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 79cbff36340878cfb6a09bbecf5b7bd93375da21
sudo add-apt-repository "deb http://apt.thehyve.net/internal/ xenial main"
sudo apt-get update
sudo apt-get install transmart-r
```
For `yum`, use the following repository url with `gpgcheck=0`: `https://repo.thehyve.nl/content/repositories/releases`.
