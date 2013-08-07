transmart-data
==============

This repository is a set of make files and scripts for:

* creating the tranSMART database and all its objects;
* populating the database with the bare minimum data necessary to start
  tranSMART and use the ETL scripts;
* generating the SQL scripts and dependency metadata necessary for the two
  points above, from a clean database;
* fixing permissions and tablespace assignments;
* importing some example data – these are intended for development, the targets
  are not robust enough to be generally applicable.

The current schema is the one necessary to support the
[`master` branch][master] on The Hyve's fork.

Oracle databases are still not supported. This goal is to have this project
displace `transmart-DB` by providing a better way to manage the tranSMART
database.

This project does not handle database upgrades and is therefore more adequate
for development. Using [Liquibase][liquibase] here or some other more ad hoc
solution for that problem is being studied.

Requirements
------------

The following are required:

* GNU make
* PostgreSQL client utilities (`psql`, `psql_dump`, etc.)
* curl
* php (>= 5.4)
* tar with support for the -J switch (GNU tar only?)
* An up-to-date checkout of the [`tranSMART-ETL` repository][ts_etl]. Revision
  e712fcd7 is necessary for Faceted Search support (ETL only)
* A copy of [Kettle][kettle] (ETL only)

Usage
-----

Start with copying the `vars.sample` file, editing it and sourcing it in:

    cp vars.sample vars
	vim vars
	. ./vars

The several options are fairly self-explanatory. The configured PostgreSQL user
must be a database superuser. If you do not intend to use the ETL targets, you
may leave `KETTLE_JOBS` and `KITCHEN` undefined and you can connect to
PostgreSQL with UNIX sockets by specifying the parent directory of the socket in
`PGHOST`.

The variable `$TABLESPACES` is the parent directory for where the tablespaces
will be created in the PostgreSQL server's filesystem.

The database creation target assumes the PostgreSQL server runs on the same
machine. The target does not attempt to create the tablespaces if they already
exist, but it will nevertheless attempt to create the directories where it
thinks the tablespaces should be according to `$TABLESPACES`.

Note that if the tablespaces directories do not already exist or are not
assigned the correct owner (i.e., the user PostgreSQL runs as), then the
install target will run into problems when attempting to create the
tablespaces. If the user PostgreSQL runs as and the user running the targets
are not the same AND the tablespace directories do not already exist, then
manual intervention is necessary for creating all the tablespaces' directories
and assigning them the correct owner.

### Drop the database

    make postgres_drop

### Create the database and load everything

    make -j4 postgres

You can skip the tablespace assignments, which are not really important for
development, by setting the environment variable `skip_fix_tablespaces` to any
non-empty value:

    skip_fix_tablespaces=1 make -j4 postgres

There's a simple script in `data/postgres/set_password.sh` for changing users'
passwords.

### Only fix permissions, owners or tablespace assignments

These can be done with the targets `fix_permissions`, `fix_owners` and
`fix_tablespaces`, under `ddl/postgres/META`. Example:

    make -C ddl/postgres/META fix_permissions

### Running ETL targets

Right now, only some sample data from the GSE8581 study is available. You can
import it like this:

    make -C samples/postgres load_clinical_GSE8581
    make -C samples/postgres load_ref_annotation_GSE8581
    make -C samples/postgres load_expression_GSE8581
    make -C samples/postgres load_analysis_GSE8581

Do not forget to update your Solr index, if your setup requires it to be
triggered manually.

The ETL functionality is not meant for any non-development purposes.

### Changing ownership or permission information

The default schema permissions are set in
`ddl/postgres/META/default_permissions.php`.  Tables where `biomart_user`
should be able to write are defined in
`ddl/postgres/META/biomart_user_write.tsv`. Extra miscellaneous permissions are
defined in `ddl/postgres/META/misc_permissions.tsv`.

Ownership information can only be added by editing an array in
`ddl/postgres/META/assign_owners.sql`.

### Generating new import files from model database

This part still needs some work, but it goes more or less like this:

* Start with a *clean* database. Preferably load if from scratch with `make
  postgres` and make only the necessary changes on top of this.
* Go to `ddl/postgres`.
* Run `make clean`.
* Manually delete the subdirectories with the names of the schemas that you
  changed.
* Run `make <schema name>_files` for every schema whose directory you deleted.
* If you have data changes, go to `data/postgres`. If you need to dump data from
  a table no data was being dumped before, add that table to one of the `<schema
  name>_list` files.
* Run `make dump`. You can run `make clean_dumps` to delete all the dumps, which
  might be useful e.g. if you deleted tables from the lists of tables that
  should be dumped.
* If you changed global objects like roles (unlikely), look at the makefile on
  `ddl/postgresql/GLOBAL` for more information.
* Make sure you test the changes by recreating the database.

  [master]: https://github.com/thehyve/transmartApp/tree/master
  [liquibase]: http://www.liquibase.com/
  [ts_etl]: https://github.com/thehyve/tranSMART-ETL
  [kettle]: http://kettle.pentaho.com/
