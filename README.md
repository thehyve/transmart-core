tranSMART Batch
============================

[![Build Status](https://travis-ci.org/thehyve/transmart-batch.svg?branch=master)](https://travis-ci.org/thehyve/transmart-batch)

tranSMART pipeline alternative to ETL, using Spring Batch.


Building
--------

    ./gradlew capsule

Executable will now be at `build/libs/transmart-batch-<version>-capsule.jar`.

Configuring
-----------

Create a file named `batchdb.properties`. Sample:

	batch.jdbc.driver=org.postgresql.Driver
	batch.jdbc.url=jdbc:postgresql://localhost:5432/transmart
	batch.jdbc.user=glopes
	batch.jdbc.password=glopes

By default, transmart-batch will look for this file in the working directory.
You can override this behavior with the `-c` option.

Preparation of the Database
---------------------------

*This step is no longer needed in new PostgreSQL instances*. Transmart-data
already creates the necessary tables for spring-batch.

On old databases (or Oracle), this needs to be done only once per database. It
creates the spring-batch schema. Running it with the capsule executable is
probably possible, but not supported. Run:

	./gradlew setupSchema

Specifying an alternative config file can be done with the Java system property
`propertySource`. A user with administrative privileges must be configured.


Running
-------

In its simplest form:

    ./transmart-batch-capsule.jar -p /path/to/STUDY_NAME/clinical.params

Beware: the parent directory of the clinical.params file will be study name. See
the usage help for more information.

Worthy of mention is that transmart-batch will refuse to re-run a job with the
same parameters (which depend on the `clinical.params` file contents). Use `-n`
to force the job to be re-run.

Details on how to prepare your datam, mapping and parameter files for transmart-batch can be found in the [docs folder](docs).

To restart a job, take a note of the job execution id at the beginning of the
failed job:

    org...BetterExitMessageJobExecutionListener - Job id is 1186, execution id is 1271

Then run:

    ./transmart-batch-capsule.jar -p /path/to/STUDY_NAME/<type>.params -r -j <execution id>

Logging can be customized by creating a `logback.groovy` file in the working
directory. You can copy the original `logback.groovy` and remove the part before
the comment `CUT HERE`:

    bsdtar -O -xvf transmart-batch-capsule.jar logback.groovy | \
	  vim -c '/CUT HERE' -c '1,+1d' -c 'wq logback.groovy' -

Development
-----------

Running can also be done without capsule:

    ./gradlew run -Pargs='-p studies/GSE8581/clinical.params -n' --debug-jvm

Before committing, run:

    ./gradlew check

To run the functional tests, you need to have a dedicated PostgreSQL database
setup. After setting it up with transmart-data, you will also need to run the
`functionalTestPrepare` target. So:

    // only once
    make -j8 postgres // in transmart-data
    ./gradlew functionalTestPrepare

    // everytime one wants to run the tests
    ./gradlew functionalTest

To test on Oracle, the procedure is similar. There is one small difference:
while the preparation step must be run with an administrator account (like with
PostgreSQL), the tests themselves must be run as `tm_cz`.
