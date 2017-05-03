Patient visit mapping (not yet implemented)
-----------------------------

The patient visit mapping file is used to provide a start and end date for each patient visit specified in your clinical data file(s). Patient visits can be present in the `PATIENT_VISIT` variable of your clinical data file(s) or [subject-sample mapping file(s)](subject-sample-mapping.md).

PATIENT_VISIT_MAP_FILE format
------------
|Subject ID     |Patient visit |Start date    |End date|
|---------------|--------------|--------------|
|subject1       |1             |3             |
|subject1       |2             |7             |
|subject2       |1             |7             |

Table, tab separated, txt file. Start and end date can be provided (e.g. 2002-08-23 11:39:00)

Description of the columns:
- `Label` **Mandatory.** The trial visit label used in clinical data file/subject-sample mapping. Must be unique.
- `Unit` **Mandatory if Value is provided.** The time unit of the trial visit label.
- `Value` **Mandatory if Unit is provided.** The value of the time unit provided in the Unit column.

Trial visit mapping upload
------------
The trial visit mapping is uploaded as part of a clinical data upload:
- Place trial visit mapping file into `clinical` folder.
- Specify trial visit mapping file inside `clinical` folder with the `TRIAL_VISIT_MAP_FILE` parameter in the `clinical.params` file.
- Run usual clinical data upload.

#### Trial visit mapping deletion
Is not currently implemented in transmart-batch. However, you can replace a label's unit and value by making the changes in your trial visit mapping file and reupload the clinical data.
