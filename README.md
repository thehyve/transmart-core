transmart-data
==============

[![Build Status](https://travis-ci.org/transmart/transmart-data.svg?branch=master)](https://travis-ci.org/transmart/transmart-data)

Introduction
------------

This repository is a set of make files and scripts for:

* creating the tranSMART database and all its objects;
* populating the database with the bare minimum data necessary to start
  tranSMART and use the ETL scripts;
* generating the SQL scripts and dependency metadata necessary for the two
  points above, from a clean database;
* fixing permissions and tablespace assignments;
* importing some example data - these are intended for development, the targets
  are not robust enough to be generally applicable;
* running the Solr cores for Faceted Search and the Sample Explorer;
* generating configuration files for tranSMART.

The current schema is the one necessary to support the
[`master` branch][master] of transmart (release 1.2) for Oracle and Postgres

This goal is to have this project displace `transmart-DB` by providing a better
way to manage the tranSMART database.

This project does not handle database upgrades and is therefore more adequate
for development. Using [Liquibase][liquibase] here or some other more ad hoc
solution for that problem is being studied.

A script is available that can compare dumps from two databases,
intended for an installed copy to be compared to the latest
transmart-data.

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
* Groovy (>= 2.1). Can be installed with `make -C env groovy` and updating the
  `PATH` (Oracle and some secondary functionality only)
* [Kettle][kettle] (ETL only)
* rsync (Solr only)
* Java environment (JDK 7)
* build-essentials tools and gcc-fortran (building R from source only)

If you are using Ubuntu and you intend to use PostgreSQL, you should be able to
install all these dependencies by running

    sudo make -C env ubuntu_deps_root
	make -C env ubuntu_deps_regular

which will also prepare some directories for the tablespaces and assign them the
correct ownership .

Usage
-----

Start with copying the `vars.sample` file, editing it and sourcing it in:

    cp vars.sample vars
	vim vars
    # edit file and save...
	. ./vars

If you ran `make -C env ubuntu_deps_regular`, you will have a `vars` file
created for you. You can skip the previous step and do only:

    . ./vars

The several options are fairly self-explanatory.

### PostgreSQL-specific notes

The configured PostgreSQL user must be a database superuser. You can connect to
PostgreSQL with UNIX sockets by specifying the parent directory of the socket
in `PGHOST`. In that case, `localhost` will be used in the situation where UNIX
sockets are not supported, such as for JDBC connections.

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

For PostgreSQL, you can skip the tablespace assignments, which are not really
important for development, by setting the environment variable
`skip_fix_tablespaces` to any non-empty value:

    skip_fix_tablespaces=1 make -j4 postgres

The Oracle version, on the other hand, does not manage tablespaces by default.
For forcing, use `ORACLE_MANAGE_TABLESPACES`:

    ORACLE_MANAGE_TABLESPACES=1 make -j4 oracle

For PostgreSQL, there's a simple script in `data/postgres/set_password.sh` for
changing users' passwords. If you're using Oracle, you can still use part of it
to generate the hashes.

### Only fix permissions, owners or tablespace assignments (PostgreSQL only)

These can be done with the targets `fix_permissions`, `fix_owners` and
`fix_tablespaces`, under `ddl/postgres/META`. Example:

    make -C ddl/postgres/META fix_permissions

### Running ETL targets

This project can be used to download public or privately available datasets and
load these into the database. Typically, but not always, this loading involves
using Kettle jobs (with kitchen) available in an existing checkout of
(tranSMART-ETL)[ts-etl] .

Data fetching is done through a system of feeds that publish several data sets.
A _data set_ is a combination of meta data and, for most data types, properly
formatted data files.  The most important meta data, the meta data that
uniquely identifies the data set, are the study name and the tranSMART data
type (clinical data, mRNA data, etc.). A _feed_ is an object that provides a
list of triplets consisting of study name, data type and location of a tarball
containing the data and full metadata for the dataset.

The tarballs must follow the following rules:

  * they must be compressed with xz;
  * their filename must be in the form `<study name>_<data type>.tar.xz`;
  * they must have a file named `<data type>.params` in their root;
  * most data types require, under the root, a directory named `<data type>`,
    under which the data files should be located.

For instance, a clinical data tarball could contain the following tree:
```
.
|-- clinical
|   |-- E-GEOD-8581.sdrf-rewrite.txt
|   |-- E-GEOD-8581_columns.txt
|   `-- E-GEOD-8581_wordmap.txt
`-- clinical.params
```

Feeds are listed in `samples/studies/public-feeds` and, optionally, in a
git-ignored `private-feeds`, in the root of the project. Each file contains a
list of feeds in the following format:
```
<feed type> <type-specific feed location data>
```

The two supported feed types right now are `http-index` and `ftp-flat`.
Examples:
```
http-index http://studies.thehyve.net/datasets_index
ftp-flat ftp://studies.thehyve.net/
```

The initial public-feeds download uses a TranSMART Foundation
host. Changing the URL in `samples/studies/public-feeds` will use an
alternative source for all data downloads.

The type `http-index` points to a plain text file that lists the available
datasets in the format `<study> <type> <tarball relative path>`. Example:
```
Cell-line acgh Cell-line/Cell-line_acgh.tar.xz
Cell-line clinical Cell-line/Cell-line_clinical.tar.xz
```

The type `ftp-flat` points to an FTP directory that should store, directly
underneath it, all the dataset tarballs.

The list of datasets is automatically downloaded when needed, but it is not
automatically updated. To update it, run at the root:

    make update_datasets

If a data set (study/type combination) is repeated, transmart-data will assume
it is the same data and will download randomly from any source. If any download
fails, another source will be tried until there is none left.

When developing the data sets, the best course of action is to place the files
directly under `samples/studies/<study name>`. The contents of this directory
should include the contents of the tarball. When finished, the tarball can be
created with (running from the root of the project):

    tar -C samples/studies/<study name> \
        -cJf <study name>_<data type>.tar.xz <data type>.params <data type>

Data sets can be loaded by running a target named `load_<data type>_<study
name>` in either `samples/oracle` or `samples/postgresql`, as appropriate.

For instance:

    make -C samples/{oracle,postgres} load_clinical_GSE8581
    make -C samples/{oracle,postgres} load_ref_annotation_GSE8581
    make -C samples/{oracle,postgres} load_expression_GSE8581
    make -C samples/{oracle,postgres} load_analysis_GSE8581

Do not forget to update your Solr index, if your setup requires it to be
triggered manually.

### Starting Solr

To start a Solr instance with one core for Faceted Search and another for the
sample explorer:

    make -C solr start

Once it is running, you can run full imports with:

	make -C solr rwg_full_import sample_full_import

The Faceted Search core also supports delta imports:

    make -C solr rwg_delta_import

Due to different functionality in tranSMART versions targetting each RDBMS,
there's a separate Solr core for Oracle:

    ORACLE=1 make -C solr start
	ORACLE=1 make -C solr sanofi_full_import

Document (e.g. PDFs) reindexing has a special procedure.

### Running Rserve

You can install a *recent* version of R from your distro and run the R scripts
inside the `R` directory to install the required packages. You can also build R
from source:

    make -C R -j8 root/bin/R
	make -C R install_packages

Beware that if you run `install_packages` with concurrency some packages will
fail building and you may have to run the command again.

There is an init script and a systemd unit available to run Rserve. It has to
be run as the same user as the tranSMART web application:

    TRANSMART_USER=<tomcat user> make -C R install_rserve_init
	update-rc.d rserve defaults 85 # to enable the service

This is applicable to the Debian-family distributions. See also the
`install_rserve_unit` target for systemd based distros.

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

### Generating new import files from model database (PostgreSQL)

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
table's data. A target is included that attempts to remedy this problem by
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

### Generating new import files from model database (Oracle)

For Oracle, only schema dumping and loading is supported:

    make -C ddl/oracle dump
	make -C ddl/oracle load

A separate branch exists that supports loading data from the TSV files dumped
by PostgreSQL.

  [master]: https://github.com/transmart/transmartApp/tree/master
  [liquibase]: http://www.liquibase.com/
  [ts_etl]: https://github.com/transmart/tranSMART-ETL
  [kettle]: http://kettle.pentaho.com/
