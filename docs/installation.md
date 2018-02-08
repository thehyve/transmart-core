---
title: Installation
---
# Installation

Below are the installation instructions for tranSMART version 17.1

  * [1. Prerequisites](#1-prerequisites)
  * [2. Setup database](#2-setup-database)
  * [3. Generate configuration files](#3-generate-configuration-files)
  * [4. Setup web application](#4-setup-web-application)
  * [5. Build and run tranSMART Server](#5-run-transmart-server)
  * [6. Build and run tranSMART Server](#5-run-transmart-server)

## 1. Prerequisites

Supported operating systems:
* Tested
	* Ubuntu 16.04
	* CentOS 7
	* ...
* Likely supported
	* ...

The module expects certain packages to be available through the package manager of the operating system:

* For all components:
    * java-1.8.0-openjdk
* For transmart-rserve:
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
* For transmart-data:
    * php
    * groovy
    * make


## 2. Setup database

Database definitions and installation instructions are in [transmart-data](transmart-data). A data loading tool based on Spring Batch is available as [transmart-batch](transmart-batch).

## 3. Generate configuration files

tranSMART requires a configuration file to be generated in `~/.grails/transmartConfig`.
Scripts to generate the configuration are shipped with
`transmart-data`.

Fetch `transmart-data`:
```
mvn dependency:get -Dartifact=org.transmartproject:transmart-data:17.1-SNAPSHOT:tar -DremoteRepositories=https://repo.thehyve.nl/content/repositories/snapshots/
mvn dependency:unpack -Dartifact=org.transmartproject:transmart-data:17.1-SNAPSHOT:tar -DoutputDirectory=.
```
To generate the configuration, please consult the documentation of [transmart-data](transmart-data).
Once a correct `vars` file has been created, the configuration can be generated and installed
with these commands (requires `php`):
```bash
pushd transmart-data-17.1-SNAPSHOT
source vars
make -C config install
popd
```

## 4. Setup web application

For production environment:

For development:

## 5. Build and run tranSMART Server

### From a source code

The project is built using [gradle](https://gradle.org/). Any version `> 2.12` and `< 2.3` should suffice (version `2.13` is recommended). Other versions may cause some build issues.
To build the project, run:
```
gradle :transmart-server:bootRepackage
```
This should create the file `transmart-server/build/libs/transmart-server-17.1-SNAPSHOT.war`.
Run it with:
```
java -jar transmart-server/build/libs/transmart-server-17.1-SNAPSHOT.war
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

## 6. Start services

`Rserve` and `Solr` services can be run using `transmart-server` (to fetch `transmart-data`, see [Generate configuration files](#3-generate-configuration-files))

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
