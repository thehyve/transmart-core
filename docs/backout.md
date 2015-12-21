Backout
==================

This job removes data from the database. This job is modular; you can choose
what data to delete.

This job will refuse to fully remove a study if it the study has data that this
job does not support removing. Beware of this limitation, as it limits the
useful of this job pending the implementation of the remaining modules.

Note that running this job requires a `backout.params` file, which is not very
convenient. You can still create an empty `backout.params` file and specify all
the parameters on the command line. E.g.:

    touch /tmp/backout.params
    ./transmart-batch-capsule.jar -p /tmp/backout.params -d STUDY_ID=GSE8581

Available parameters
--------------------
- `INCLUDED_TYPES` -- the modules to include, comma separated. Cannot be
  specified if `EXCLUDED_TYPES` is specified. If neither is specified, defaults
  to all the modules. The `full` module that cannot be explicitly included (the
  only way to run it is to leave `INCLUDED_TYPES` and `EXCLUDED_TYPES` blank).
- `EXCLUDED_TYPES` -- include all the modules except those included in this
  comma separated list. The module `full` is automatically excluded if this
  parameter is not blank. See also `INCLUDED_TYPES`.

You could also use [the study-specific parameters](study-params.md).


Overview
--------

This job will run a few common steps at the beginning and at the end. In the
middle, it will run sequentially the specified modules. Each module has two
phases -- in the first, it determines whether data whose deletion it handles
exists on the database; the second phase is only invoked if such data indeed
exists and it handles the data's deletion.

The `full` module is special. It always runs last and it *aborts* the job if it
finds concepts or assays belonging to the study in question (apart from the top
node). If it doesn't, it proceeds to the deletion of the top node and the study
patients. No other module deletes patients, since data for _all_ of the data
types depends on them being present.


Modules
-------

Available modules at this point:

  * `clinical` -- deletes clinical data and clinical data related only concepts.
    Does *not* delete patients.
  * `full` -- deletes the study top node and the study patients, provided no
    other data remains.
