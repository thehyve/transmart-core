# transmart-upgrade
[![Build Status](https://travis-ci.org/thehyve/transmart-upgrade.svg?branch=master)](https://travis-ci.org/thehyve/transmart-upgrade/branches)

Temporary repo for sharing work on transmart upgrade to grails 3.1.10

TODO LIST:
- merge DataSource.groovy and config.groovy into application.groovy inside grails-app/conf/
- replace audit metric before and after with interceptors
- move in fixes for change in hibernate classes paths (I have fix locally in a separate repo - I will bring it in ~Piotr)
- migrate BuildConfig.groovy to build.gradle
- make core-db-tests pass after upgrade
- apply gradle modules architecture to the project
- migrate selected repositories after upgrade to a single one called transmart
- adapt .travis.yml
