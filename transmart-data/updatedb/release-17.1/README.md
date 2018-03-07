Database update scripts for release 17.1
========================================

Overview
--------

## Database structure changes

### SNP Data

| Script                             | Description                                     |
|------------------------------------|-------------------------------------------------|
| `de_snp_subject_sorted_def.sql`    | Adds assay_id column                            |
| `de_snp_subject_sorted_def_bk.sql` | Adds assay_id and bio_assay_platform_id columns |
| `deapp_pf_to_tf.sql.sql`           | Adds 'deapp' schema related changes             |
| `i2b2_load_security_bystudy.sql`   | Creates a new procedure for Oracle              |
| `i2b2_create_genotype_tree.sql.sql`| Creates a new procedure for Oracle              |

### GWAS
| Script                             | Description                           |
|------------------------------------|---------------------------------------|
| `biomart_pf_to_tf.sql`             | Adds 'biomart' schema related changes |
| `bio_marker_correl_mv.sql`         | Updates bio marker correlation view   |

### New HD data type. RNA-Seq Transcript Level

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `de_rnaseq_transcript_annot.sql` | Creates the annotation table |
| `de_rnaseq_transcript_data.sql` | Creates the data table       |

### External workflow systems support. e.g. Arvados

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `supported_workflow.sql` | Creates a table to store work flows |
| `storage_system.sql` | Creates a table to store storage systems |
| `linked_file_collection.sql` | Creates a table to store file collections |

### Dimension support

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `dimension_description.sql` | Creates a table to store dimensions |

### Study dimension

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `study.sql` | Creates the study table |
| `study_dimension_descriptions.sql` | Creates a table to store supported dimensions by a study |

### Trial visit dimension

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `trial_visit_dimension.sql` | Creates a new dimension table, trial visit table |
| `observation_fact.sql`      | Adds a TRIAL_VISIT_NUM column, makes the column a part of the OBSERVATION_FACT_PKEY|

## Data Migration

`data_migration.sql` takes care of filling in dimension descriptions and transforming existing data to the new way to store data.
See script's comments for more details.

There is data migration scripts that is not included into the automatic migration.
It's `oracle/merge_concepts.sql`. It merges several terms you specify in the script into one.
You might consider to run it manually.

### Storing patient set query inside qt_query_master table.

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `qt_query_master.sql` 	   | Add REQUEST_CONSTRAINTS and API_VERSION columns |

### Data export job.

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `async_job.sql` 	   | Add USER_ID column |

### Variable type in visual attributes

The type of a variable is now stored as visual attributes in the i2b2 tree,
so that the type does not have to be derived from the XML in the metadata blob.

Only `i2b2_secure` is updated, as the `i2b2` table is redundant and is considered deprecated.

`data_migration.sql` takes care of adding the appropriate visual attributes to leaf nodes.
See script's comments for more details.

### Managed tag types

For managing vocabularies used in certain tag types, two tables have been introduced:
`i2b2_tag_types` and `i2b2_tag_options`, and a new column `option_id` in the
`i2b2_tags` table.

### User queries

Patients and observations queries to support front end functionality are now stored in the `query` table.

How to apply all changes
------------------------

Given that transmart-data is configured correctly you could run the following make command

For PostgreSQL:
    
    make -C postgres migrate
    
For Oracle:

    make -C oracle migrate
