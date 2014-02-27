#TranSMART Core-DB#

[![Build Status](https://travis-ci.org/thehyve/transmart-core-db.png?branch=master)](https://travis-ci.org/thehyve/transmart-core-db)

The transmart-core-db project is a Grails plugin that implements the API
described in transmart-core-api. It should be included as a runtime dependency
for the transmartApp.

This plugin also includes additional functionality besides implementing the API,
like defining controllers on which the application relies. Gradually, we will
want to refactor these components into separate plugins.

##Running the integration tests##

Ideally, one would run the integration tests against a temporary in-memory
database. This is the default. The grails-app/conf/DataSource.groovy has an H2
data source configured.

However, transitional considerations recommend also running the integration
tests against a production database (PostgreSQL or Oracle). For this, you can
either change the in-tree DataSource.groovy file mentioned before or you can
(preferably) create a file in
<tt>~/.grails/transmartConfig/DataSource.groovy</tt> where you override the
in-tree configuration. This file has the same format as the in-tree one. The
last option is preferred because it avoids accidental commits to the in-tree
file.
