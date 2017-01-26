# tranSMART
[![Build Status](https://travis-ci.org/thehyve/transmart-upgrade.svg?branch=master)](https://travis-ci.org/thehyve/transmart-upgrade/branches)

Repository containing the core components and documentation of the _tranSMART_ platform,
an open source data sharing and analytics platform for translational biomedical research, maintained
by the [tranSMART Foundation](http://transmartfoundation.org).

## Overview

The platform provides an API, which is available under [transmart-rest-api](transmart-rest-api).
Its `v1` endpoints are documented there, the `v2` endpoints are documented using Swagger in [open-api](open-api).
There is a frontend application [transmartApp](transmartApp) built on Grails, and an Angular based
front end named [glowing bear](https://github.com/thehyve/transmart-base-ui) is being developed.
The OAuth2 authentication of the API is currently also part of `transmartApp`.

Database definitions and installation instructions are in [transmart-data](transmart-data).
A data loading tool based on Spring Batch is available as [transmart-batch](transmart-batch).

## Build and run

The project is built using [gradle](https://gradle.org/). Any version `> 2.12` should suffice.
To build the project, run:
```
gradle :transmartApp:bootRepackage
```
This should create the file `transmartApp/build/libs/transmartApp-17.1.war`.
Run it with:
```
java -jar transmartApp/build/libs/transmartApp-17.1.war
```

The application expects configuration in `~/.grails/transmartConfig`. Check [transmart-data](transmart-data) on how to set up the database and generate the required configuration files.

## Git history

This repository is a merge of several, previously separated, repositories from [github.com/transmart](https://github.com/transmart/).
The history of those repositories is merged in as well (branch [transmart-history](../../tree/transmart-history)), but the current `master` branch is disconnected from
these histories. The master branch can locally be connected to the history with `git replace`.
```bash
# replace the master-base object with transmart-history
git replace 58a48ff dd57ce1
```

## License

Copyright &copy; 2008-2017  The tranSMART developers.
See the [COPYRIGHT](COPYRIGHT) file.

tranSMART is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the [GNU General Public License](gpl-3.0.txt) along with this program. If not, see https://www.gnu.org/licenses/.


Some subprojects may have more permissive licenses. Check the individual projects and files for details.

