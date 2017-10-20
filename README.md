# tranSMART
[![Build Status](https://travis-ci.org/thehyve/transmart-core.svg?branch=master)](https://travis-ci.org/thehyve/transmart-core/branches)

This is the repository containing the core components and documentation of the _tranSMART_ platform,
an open source data sharing and analytics platform for translational biomedical research. tranSMART
is maintained by the [tranSMART Foundation](http://transmartfoundation.org). Official releases
can be found on the tranSMART Foundation website, and the tranSMART Foundation's development repositories
can be found at <https://github.com/transmart/>.

## Overview

The platform provides an API, which is available under [transmart-rest-api](transmart-rest-api).
Its `v1` endpoints are documented there, the `v2` endpoints are documented using Swagger in [open-api](open-api).
There is a main application [transmart-server](transmart-server), exposing the API and extended by multiple plugins. As an user interface there is a frontend application [transmartApp](transmartApp) built as a "web" profiled Grails platform plugin, and an Angular4 based
front end named [glowing bear](https://github.com/thehyve/glowing-bear) is being developed.
The OAuth2 authentication of the API is managed by [transmart-oauth](transmart-oauth) plugin.

Database definitions and installation instructions are in [transmart-data](transmart-data).
A data loading tool based on Spring Batch is available as [transmart-batch](transmart-batch).

## Relation to other transmart repositories

Before version 17.1, the tranSMART source code was split over a number of different repositories with names such
as transmartApp, transmart-core-api, transmart-core-db, transmart-rest-api, RModules, etc. As of version 17.1 the
components that make up the core server have been merged into a single repository. If you want the source for
tranSMART 16.x or older, look at the separate repositores, if you want the sources for version 17+, you will want
this repository.

## Build and run

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

The application expects configuration in `~/.grails/transmartConfig`. Check [transmart-data](transmart-data) on how to set up the database and generate the required configuration files.

## Deployment

Deployment artefacts are published to [the Nexus repository of The Hyve](https://repo.thehyve.nl/).

### Fetch and run `transmart-server`: 
```bash
# Fetch artefacts using Maven 
mvn dependency:get -Dartifact=org.transmartproject:transmart-server:17.1-SNAPSHOT:war -DremoteRepositories=https://repo.thehyve.nl/content/repositories/snapshots/,https://repo.grails.org/grails/core
mvn dependency:copy -Dartifact=org.transmartproject:transmart-server:17.1-SNAPSHOT:war -DoutputDirectory=.
# Start the web application
java -jar transmart-server-17.1-SNAPSHOT.war
```

### Fetch `transmart-data`, configure, start services
tranSMART also requires a configuration file to be generated in `~/.grails/transmartConfig`
and `Rserve` and `Solr` to run.
Scripts to generate the configuration and to start `Rserve` and `Solr` are shipped with
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


## Git history

This repository is a merge of several, previously separated, repositories from [github.com/transmart](https://github.com/transmart/).
The history of those repositories is merged in as well (branch [transmart-history](../../tree/transmart-history)), but the current `master` branch is disconnected from
these histories. The master branch can locally be connected to the history with `git replace`.
```bash
# replace the master-base object with transmart-history
git replace 58a48ff dd57ce1
```

## License

Copyright &copy; 2008-2017
See the [COPYRIGHT](COPYRIGHT) file.

tranSMART is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the [GNU General Public License](LICENSE) along with this program. If not, see https://www.gnu.org/licenses/.


Some subprojects may have more permissive licenses. Check the individual projects and files for details.

