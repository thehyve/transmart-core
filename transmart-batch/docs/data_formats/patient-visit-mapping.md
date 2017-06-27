Patient visit mapping (not yet implemented)
-----------------------------

The patient visit mapping file is used to provide a start and end date for each patient visit specified in your clinical data file(s). Patient visits can be present in the `PATIENT_VISIT` variable of your clinical data file(s) or [subject-sample mapping file(s)](subject-sample-mapping.md).

PATIENT_VISIT_MAP_FILE format
------------
|Subject ID     |Patient visit |Start date    |End date   |
|---------------|--------------|--------------|-----------|
|subject1       |1             |2016-08-23    |2016-08-24 |
|subject1       |2             |2016-11-03    |           |
|subject2       |1             |2016-07-19    |2016-07-21 |

Table, tab separated, txt file. Start and end date should be provided in YYYY-MM-DD format and may be acompanied by the time of day in HH:MM:SS format (e.g. 2016-08-23 11:39:00).

Description of the columns:
- `Subject ID` **(Mandatory)** The subject ID as they are defined in the `SUBJ_ID` variable of your clinical data file(s).
- `Patient visit` **(Mandatory)** Any integer value that discerns a subject's patient visits from one another. Has to match with the patient visit integers used in your `PATIENT_VISIT` variable of either clinical data file(s) or subject-sample mapping.
- `Start date` The date(time) value of the start of the patient visit.
- `End date` The date(time) value of the end of the patient visit.

Patient visit mapping upload
------------
The patient visit mapping is uploaded as part of a clinical data upload:
- Place patient visit mapping file into `clinical` folder.
- Specify patient visit mapping file inside `clinical` folder with the `PATIENT_VISIT_MAP_FILE` parameter in the `clinical.params` file.
- Run usual clinical data upload.

#### Patient visit mapping deletion
Is not currently implemented in transmart-batch. However, you can replace a subject's patient visit start and end date by making the changes in your patient visit mapping file and reupload the clinical data.
