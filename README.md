transmart-data
==============

A short list of the most useful commands
------------

DDL
---

Dump database ddl schema

    make -C ddl/oracle dump
    make -C ddl/postgres dump

Create database database ddl structure from dumped scripts

    make -C ddl/oracle load
    make -C ddl/postgres load

Reference data
--------------

Dump reference data to tsv files from the tables specified in `data/common/<schema_name>_list`

    make -C data/oracle dump
    make -C data/postgres dump

Upload reference data from the tsv files

    make -C data/oracle load
    make -C data/postgres load

DDL + Reference data
--------------------

Create database and upload reference data

    make oracle
    make postgres

Drop database

    make oracle_drop
    make postgres_drop

Introduction
------------

This repository is a set of make files and scripts for:

* creating the tranSMART database and all its objects;
* populating the database with the bare minimum data necessary to start
  tranSMART and use the ETL scripts;
* generating the SQL scripts and dependency metadata necessary for the two
  points above, from a clean database;
* fixing permissions and tablespace assignments;
* importing some example data – these are intended for development, the targets
  are not robust enough to be generally applicable;
* running the Solr cores for Faceted Search and the Sample Explorer;
* generating configuration files for tranSMART.

The current schema is the one necessary to support the
[`master` branch][master] on The Hyve's fork.

This goal is to have this project
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
* [Kettle][kettle] (ETL only)
* rsync (Solr only)
* Java environment (JDK) (Solr only)

If you are using Ubuntu, you should be able to install all these dependencies
by running

    sudo make -C env ubuntu_deps_root
	make -C env ubuntu_deps_regular

which will also prepare some directories for the tablespaces and assign them the
correct ownership.

Usage
-----

Start with copying the `vars.sample` file, editing it and sourcing it in:

    cp vars.sample vars
	vim vars
    # edit file and save...
	. ./vars

If you ran `make -C env ubuntu_deps_regular`, you will have a `vars-ubuntu` file
created for you. You can skip the previous step and do only:

    . ./vars-ubuntu

The several options are fairly self-explanatory.

The configured PostgreSQL user
must be a database superuser. You can connect to PostgreSQL with UNIX sockets by
specifying the parent directory of the socket in `PGHOST`. In that case,
`localhost` will be used in the situation where UNIX sockets are not supported,
such as for JDBC connections.

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

    make oracle_drop

### Create the database and load everything

    make -j4 postgres

    make -j4 oracle

You can skip the tablespace assignments, which are not really important for
development, by setting the environment variable `skip_fix_tablespaces` to any
non-empty value:

    skip_fix_tablespaces=1 make -j4 postgres

Oracle version in oposite do not manage tablespaces by default.
Fo forcing use `ORACLE_MANAGE_TABLESPACES`:

    ORACLE_MANAGE_TABLESPACES=1 make -j4 oracle

There's a simple script in `data/postgres/set_password.sh` for changing users'
passwords.

### Only fix permissions, owners or tablespace assignments

These can be done with the targets `fix_permissions`, `fix_owners` and
`fix_tablespaces`, under `ddl/postgres/META`. Example:

    make -C ddl/postgres/META fix_permissions

### Running ETL targets

Right now, only some sample data from the GSE8581 study is available. You can
import it like this:

    make -C samples/{oralce,postgres} load_clinical_GSE8581
    make -C samples/{oralce,postgres} load_ref_annotation_GSE8581
    make -C samples/{oralce,postgres} load_expression_GSE8581
    make -C samples/{oralce,postgres} load_analysis_GSE8581

Do not forget to update your Solr index, if your setup requires it to be
triggered manually.

The ETL functionality is not meant for any non-development purposes.

### Starting Solr

To start a Solr instance with one core for Faceted Search and another for the
sample explorer:

    make -C solr start

Once it is running, you can run full imports with:

	make -C solr rwg_full_import sample_full_import

The Faceted Search core also supports delta imports:

    make -C solr rwg_delta_import

### Changing ownership or permission information

The default schema permissions are set in
`ddl/postgres/META/default_permissions.php`.  Tables where `biomart_user`
should be able to write are defined in
`ddl/postgres/META/biomart_user_write.tsv`. Extra miscellaneous permissions are
defined in `ddl/postgres/META/misc_permissions.tsv`.

Ownership information can only be added by editing an array in
`ddl/postgres/META/assign_owners.sql`.

### Writing the configuration files

For development, the default configuration should be sufficient. For production,
the configuration may have to be changed, but even if it doesn't, some
directories described in the configuration will need to be created.

If you need to change configuration parameters, you can change the files in
`~/.grails/transmartConfig` that are createdi by the `install` target, but they
will be overwritten (after being backed up) the next time you install the
configuration again using the same target. Therefore, it is preferrable to copy
`config/Config-extra.php.sample` into `config/Config-extra.php` and edit the new
file. In this file, you can edit two blocks of text which will be inserted into
two different points in the configuration template, allowing you override any
option that the configuration template sets.

To install the configuration files into `$TSUSER_HOME/.grails/transmartConfig`,
(`$TSUSER_HOME` is one of the environment variables defined in the `vars` file)
run:

    make -C config install

### Generating new import files from model database (Postgresql)

This part still needs some work, but it goes more or less like this:

* Start with a *clean* database. Preferably load if from scratch with `make
  postgres` and make only the necessary changes on top of this.
* Go to `ddl/postgres`.
* Run `make clean`.
* Manually delete the subdirectories with the names of the schemas that you
  changed.
* Run `make <schema name>_files` for every schema whose directory you deleted.
* (If you want to regenerate all the schemas, you can replace the last three
  steps with `make clean_all dump files_all`).
* If you have data changes, go to `data/postgres`. If you need to dump data from
  a table no data was being dumped before, add that table to one of the `<schema
  name>_list` files.
* Run `make dump`. You can run `make clean_dumps` to delete all the dumps, which
  might be useful e.g. if you deleted tables from the lists of tables that
  should be dumped.
* If you changed global objects like roles (unlikely), look at the makefile on
  `ddl/postgresql/GLOBAL` for more information.
* Make sure you test the changes by recreating the database.

Note that pg\_dump does not output the data in any specific order; after a
table is modified, it will dump the old data out of order. This results in a
larger changeset than necessary and obfuscates the actual changes made to the
tableʼs data. A target is included that attempts to remedy this problem by
changing the modified files so that the old rows are kept at the top of the
modified file in the same order (but with the removed rows left out):

    make -C data/common minimize_diffs

Use this target after modifying the tsv files but before checking them into the
git index with `git add`. The procedure compares the version in the working
directory with the version in the git index.

To update the plugin module params from the database (a doubtful course of
action; better to edit the files in
`data/common/searchapp/plugin_modules_params/`), then do:

    cd data/postgres
	make searchapp/Makefile
    make -C searchapp dump_plugin_module
    make -C ../common/searchapp/plugin_modules_params process_dump

  [master]: https://github.com/thehyve/transmartApp/tree/master
  [liquibase]: http://www.liquibase.com/
  [ts_etl]: https://github.com/thehyve/tranSMART-ETL
  [kettle]: http://kettle.pentaho.com/
