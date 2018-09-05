# tranSMART
[![Build Status](https://travis-ci.org/thehyve/transmart-core.svg?branch=master)](https://travis-ci.org/thehyve/transmart-core/branches)

This is the repository containing the core components and documentation of the _tranSMART_ platform,
an open source data sharing and analytics platform for translational biomedical research. tranSMART
is maintained by the [i2b2 tranSMART Foundation](http://transmartfoundation.org). Official releases
can be found on the Foundation website, and the Foundation's development repositories
can be found at <https://github.com/transmart/>.

All the instructions on how to install, build and run a private instance of tranSMART, get set up for developing or upgrade to the latest version of tranSMART from an older version are available [in the documentation.](docs/README.md)

For details on contributing code changes via pull requests, [see the Contributing document.](CONTRIBUTING.md)

## Overview

The platform provides an API, which is available under [transmart-rest-api](transmart-rest-api).
Its `v1` endpoints are documented there, the `v2` endpoints are documented using Swagger in [open-api](open-api).
There are two main applications available: [transmart-server](transmart-server) and [transmart-api-server](transmart-api-server), both exposing the API and extended by multiple plugins.
The OAuth2 authentication of the API for transmart-server is managed by [transmart-oauth](transmart-oauth) plugin, while transmart-api-server uses an external identity provider - [Keycloak](https://www.keycloak.org/).

As an user interface for transmart-server there is a frontend application [transmartApp](transmartApp) built as a "web" profiled Grails platform plugin. There is also a modern cohort selector for i2b2 tranSMART named [glowing bear](https://github.com/thehyve/glowing-bear) that can used both with transmart-server and transmart-api-server.


Database definitions and installation instructions are in [transmart-data](transmart-data).
A data loading tool based on Spring Batch is available as [transmart-batch](transmart-batch).
There is also a PostgreSQL databases specific data uploader tool - [transmart-copy](transmart-copy).


## Relation to other tranSMART repositories and Git history

This repository is a merge of several, previously separated, repositories from [github.com/transmart](https://github.com/transmart/),
with names such as transmartApp, transmart-core-api, transmart-core-db, transmart-rest-api, RModules, etc.
As of version 17.1, the components that make up the core server have been merged into this repository.
If you want the source for tranSMART 16.x or older, look at the separate repositores.

The history of those repositories is merged into this one as well (branch [transmart-history](../../tree/transmart-history)),
but the current `master` branch is disconnected from these histories.
The master branch can locally be connected to the history with `git replace`.
```bash
# replace the master-base object with transmart-history
git replace 58a48ff dd57ce1
```

## License

Copyright &copy; 2008-2018
See the [COPYRIGHT](COPYRIGHT) file.

tranSMART is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the [GNU General Public License](LICENSE) along with this program. If not, see https://www.gnu.org/licenses/.


Some subprojects may have more permissive licenses. Check the individual projects and files for details.

