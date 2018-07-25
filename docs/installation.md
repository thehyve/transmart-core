---
title: tranSMART 17.1 Installation
---
# tranSMART 17.1 Installation

Below are the installation instructions for tranSMART version 17.1.

  * [1. Prerequisites](#1-prerequisites)
  * [2. Setup database](#2-setup-database)
  * [3. Generate configuration files](#3-generate-configuration-files)
  * [4. Build and run tranSMART Server](#4-run-transmart-server)
  * [5. Start services](#5-start-services)


## 1. Prerequisites

Supported operating systems (some others are likely supported, but not tested):
* Ubuntu 16.04 and 18.04
* CentOS 7
* MacOS Sierra

The module expects certain softwares and libraries to be present on the system.
They are listed below, the versions mentioned are known to work but others might
as well.

* For all components:
    * JDK 8
* For building tranSMART from sources:    
    * [gradle] 2.13. Any version `> 2.12` and `<= 3.5.1` should suffice (version `3.5.1` is recommended). Other versions may cause some build issues.
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
* [Create the database and load everything](../transmart-data#create-the-database-and-load-everything)
 
And then a data loading tool based on Spring Batch is available as [transmart-batch](../transmart-batch).


## 3. Generate configuration files

tranSMART requires a configuration file to be generated in `~/.grails/transmartConfig`.
Scripts to generate the configuration are shipped with
`transmart-data`.

Either use the sources in this repository (`transmart-core/transmart-data`),
or fetch `transmart-data` from a Nexus repository:
```
mvn dependency:get -Dartifact=org.transmartproject:transmart-data:17.1-SNAPSHOT:tar -DremoteRepositories=https://repo.thehyve.nl/content/repositories/snapshots/
mvn dependency:unpack -Dartifact=org.transmartproject:transmart-data:17.1-SNAPSHOT:tar -DoutputDirectory=.
```
To generate the configuration, please consult the documentation of [transmart-data](../transmart-data).
Once a correct `vars` file has been created, the configuration can be generated and installed
with these commands (requires `php`):
```bash
pushd transmart-data-17.1-SNAPSHOT
source vars
make -C config install
popd
```


## 4. Build and run tranSMART Server

### From source code

The project is built using [gradle].
To build the project, run:
```
gradle :transmart-server:assemble
```
This should create the file `transmart-server/build/libs/transmart-server-17.1-SNAPSHOT.war`.
Run it in production mode with:
```
java -jar transmart-server/build/libs/transmart-server-17.1-SNAPSHOT.war
```
Or in development mode with:
```
cd transmart-server
grails run-app
```

### From a Nexus repository

Deployment artefacts are published to [the Nexus repository of The Hyve](https://repo.thehyve.nl/).

To fetch and run `transmart-server`:
```bash
# Fetch artefacts using Maven
mvn dependency:get -Dartifact=org.transmartproject:transmart-server:17.1-SNAPSHOT:war -DremoteRepositories=https://repo.thehyve.nl/content/repositories/snapshots/,https://repo.grails.org/grails/core
mvn dependency:copy -Dartifact=org.transmartproject:transmart-server:17.1-SNAPSHOT:war -DoutputDirectory=.
# Run it with:
java -jar transmart-server-17.1-SNAPSHOT.war
```

####
In order to build and run **tranSmart Api Server**, follow the instructions above, replacing the `transmart-server` with `transmart-api-server`.


## 5. Start services

`Rserve` and `Solr` services can be run using `transmart-data`, see [Generate configuration files](#3-generate-configuration-files)
to fetch it from a Nexus repository. 
These instructions are using downloaded binaries, see the documentation of [transmart-data](../transmart-data) if you want 
to use the sources. 
Note that `Rserve` and `Solr` are not necessarily needed for a development installation.

Start `Solr`:
```bash
pushd transmart-data-17.1-SNAPSHOT/solr
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


[gradle](https://gradle.org)
