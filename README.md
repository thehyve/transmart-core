tranSMART Batch
============================

tranSMART pipeline alternative to ETL, using Spring Batch.


Building
--------

    ./gradlew capsule

Executable will now be at `build/libs/transmart-batch-capsule.jar`.

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

This needs to be done only once per database. It creates the spring-batch
schema. Running it with the capsule executable is probably possible, but not
supported. Run:

    ./gradlew setupSchema

Specifying an alternative config file can be done with the Java system property
`propertySource`.


Running
-------

In its simplest form:

    ./transmart-batch-capsule.jar -p /path/to/STUDY_NAME/clinical.params

Beware: the parent directory of the clinical.params file will be study name. See
the usage help for more information.

Worthy of mention is that transmart-batch will refuse to re-run a job with the
same parameters (which depend on the `clinical.params` file contents). Use `-n`
to force the job to be re-run.


Development
-----------

Running can also be done without capsule:

    ./gradlew run -Pargs='-p studies/GSE8581/clinical.params -n' --debug-jvm

Before committing, run:

    ./gradlew check

To run the functional tests, you need to have a dedicated PostgreSQL database
setup. After setting it up with transmart-data, besides running the
`setupSchema` target, you will also need to run the `functionalTestPrepare`
target. So:

    // only once
	make -j8 postgres // in transmart-data
    ./gradlew setupSchema functionalTestPrepare

	// everytime one wants to run the tests
    ./gradlew functionalTest
