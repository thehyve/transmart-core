Trial visit mapping (not yet implemented)
-----------------------------

The trial visit mapping file is used to map the trial visit labels to their corresponding time unit and its value. Trial visit labels can be present in the `TRIAL_VISIT_LABEL` variable of your clinical data file(s) and the `TIME_POINT` variable in [subject-sample mapping file(s)](subject-sample-mapping.md). Although not mandatory for incorporating trial visit data in your study, only in this file can the time unit and value be specified.

TRIAL_VISIT_MAP_FILE format
------------
|Label          |Unit     |     Value    |
|---------------|---------|--------------|
|Baseline       |Days     |3             |
|Week1          |Days     |7             |
|Week2          |Days     |7             |

Table, tab separated, txt file. Labels specified in this file that do not occur in any clinical data/subject-sample mapping file will be ignored.

Description of the columns:
- `Label` **Mandatory.** The trial visit label used in clinical data file/subject-sample mapping. Must be unique.
- `Unit` **Mandatory if Value is provided.** The time unit of the trial visit label.
- `Value` **Mandatory if Unit is provided.**The value of the time unit provided in the Unit column.

Trial visit mapping upload
------------
The trial visit mapping is uploaded as part of a clinical data upload:

    * Place trial visit mapping file into `clinical` folder.
    * Specify trial visit mapping file inside `clinical` folder with the `TRIAL_VISIT_MAP_FILE` parameter in the `clinical.params` file.
    * Run usual clinical data upload.

##### Tags deletion
Is not currently implemented in transmart-batch. However, you can replace a label's unit and value by making the changes in your trial visit mapping file and reupload the clinical data.
