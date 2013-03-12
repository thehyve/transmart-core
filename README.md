#TranSMART Core-DB#

The transmart-code-db project is a Grails plugin that implements the API
described in transmart-core-api. It should be included as a runtime dependency
for the transmartApp.

This plugin also includes functionality besides implementing the API, like
defining controllers on which the application relies. Gradually, we will want to
refactor these components into separate plugins.

##Running the integration tests##

Ideally, one would run the integration tests against a temporary in-memory
database. However, transitional considerations make it imperative that the code
is exercised against the current schema, as the database schema is not managed
by Hibernate.

The tests are written in a way that they should pass should extra data exist on
the database. No preference is expressed as to where the tests should be run –
this must be configured explicitly. To run the tests against a temporary
database, uncommenting the relevant data source in
<tt>grails-app/conf/DataSource.groovy</tt> for the test environment should be
sufficient.  Be warned, however, that this module includes the same out-of-tree
data source configuration file as transmartApp and that that file (i.e.
<tt>~/.grails/transmart/DataSource.groovy</tt>) may override your preference in
the aforementioned DataSource.groovy file in grails-app. If you prefer not to
pollute the tree and risk committing unintended changes in
<tt>grails-app/conf/DataSource.groovy</tt>, you can also add your data source
configuration in the out-of-tree file. It has the advantage that you can just
define your postgres data source for both transmartApp and core-db in the same
file (in which case you will be testing against postgres, not h2). 
