Trial visit mapping (not yet implemented)
-----------------------------

The trial visit mapping file is used to map the trial visit labels to their corresponding time unit and its value. Trial visit labels can be present in the `TRIAL_VISIT_LABEL` variable of your clinical data file(s) and the `TIME_POINT` variable in [subject-sample mapping file(s)](subject-sample-mapping.md).

TRIAL_VISIT_MAP_FILE format
------------

|Label          |Unit     |     Value    |
|---------------|---------|--------------|
|Baseline       |Days     |3             |
|Week1          |Days     |7             |

Table, tab separated, txt file. It contains information about concepts and the ontology tree that are uploaded to TranSMART. The first two columns refer to columns in the column mapping file. The rows were these columns are not empty, are used for associating observations with concept codes. The others are only used to build the tree.

Description of the columns:
- `Label`  The category name used in the column mapping file.
- `Unit`  The variable name used in the column mapping file.
- `Value`  Concept code to be used to classify observations.

#####Tags upload.

You have two ways to upload tags:

- As part of clinical data upload.

    * Place tags file into `clinical` folder.
    * Specify tags file inside `clinical` folder with `TAGS_FILE` variable inside `clinical.params` file.
    * Run usual clinical data upload.

- As separate tags data type upload.

    * Place tags file into `tags` folder.
    * You must specify tags file inside `tags` folder with `TAGS_FILE` variable inside `tags.params` file only if you
    have several files inside `tags` folder.
    * Run

        ./transmart-batch-capsule.jar -p /path/to/STUDY_NAME/tags.params

#####Tags deletion.
Is not implemented in transmart-batch.
You could delete tags with following sql: `delete from i2b2metadata.i2b2_tags where path like '<path>' and tag_type='<title>'`
