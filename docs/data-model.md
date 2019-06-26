# TranSMART data model

## TranSMART database model

This diagram gives an overview of the core tables in the TranSMART database.
The central table stores observations, which are linked to patients, concepts,
visits, trial visits and studies.

![TranSMART database diagram](model/transmart-database/transmart%20database%20diagram.jpg)

Detailed database documentation is available from
the [database documentation](https://thehyve.github.io/transmart-core-db-doc) generated with SchemaSpy.
This is based on the database that is generated with the legacy [transmart-data](../transmart-data) scripts.
Alternative (preferred) database creation scripts, based on Liquibase,
are available in [transmart-schemas](../transmart-schemas)

Important tables currently missing in the diagram are:
- tables used to (temporarily) store patient sets
- tables used to store relations between patients.


## Conceptual model

The following diagram gives a more high level view of the core of the TranSMART data model,
which may be more useful for understanding the core entities and their relations.

Here again, the observations are the core objects in the data model, linked to
patients, concepts, visits, trial visits and studies. The relation between tree nodes
and either studies or concepts is made more explicit. Also, the metadata linked
to observations is made more explicit.

![TranSMART data model](model/transmart/transmart%20class%20diagram.jpg)

