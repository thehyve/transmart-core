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

### New HD data type. RNA-Seq Transcript Level

| Script                           | Description                  |
|----------------------------------|------------------------------|
| `de_rnaseq_transcript_annot.sql` | Creates the annotation table |
| `de_rnaseq_transcript_annot.sql` | Creates the data table       |

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
| `trial_visit_dimension.sql` | Creates a new dimension table, trial visit table |
| `study.sql` | Creates the study table |
| `study_dimension_descriptions.sql` | Creates a table to store supported dimensions by a study |

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


How to apply all changes
------------------------

Given that transmart-data is configured correctly you could run the following make command

For PostgreSQL:
    
    make -C postgres migrate
    
For Oracle:

    make -C oracle migrate
