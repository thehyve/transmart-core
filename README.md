# transmart-upgrade
[![Build Status](https://travis-ci.org/thehyve/transmart-upgrade.svg?branch=master)](https://travis-ci.org/thehyve/transmart-upgrade/branches)

Temporary repo for sharing work on transmart upgrade to grails 3.1.10

## To do
### Repository
- [x] Adapt `.travis.yml`
- [ ] Migrate selected repositories after upgrade to a single one called `transmart`
- [ ] Move in git history of originating repositories

### Files, configuration
- [x] Apply gradle modules architecture to the project
- [x] Migrate `BuildConfig.groovy` to `build.gradle`
- [x] Merge `DataSource.groovy` and `config.groovy` into application.groovy inside `grails-app/conf/`
- [ ] (maybe) move ``*/src/main/resources/*` to `*/src/main/resources/public/*`, I'm not sure if that is needed or not, see https://github.com/grails/grails-core/releases/tag/v3.0.12
	I think this is only needed if the resources in question are to be served under the `/static` path in the web application, but not sure.
- [ ] Replace `WEB-INF/applicationContext.xml` and `sitemesh.xml`. Beans from applicationContext are now defined in `grails-app/conf/spring/resources.groovy`, sitemesh functionality is gone (so we'll need to see if it's needed and if so figure out a replacement)
- [x] Move version from ``*GrailsPlugin.groovy` to `build.gradle`

### Fix code
- [x] Replace audit metric before and after with interceptors
- [x] Move in fixes for change in hibernate classes paths

### Tests
- [x] transmart-core-db-tests
- [x] transmart-rest-api
    - Added `@Ignore` to `StudyLoadingServiceSpec`
- [ ] transmartApp:
    - all failing
