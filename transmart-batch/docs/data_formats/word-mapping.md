Word mapping
================

The word mapping is an optional part of the clinical data upload process. It can be used in case a codebook already exists, or simply to change data labels without editing the source data. Through the word mapping it is possible to assign a different value to a categorical variable for upload to tranSMART. The name of this file must be specified at the `WORD_MAP_FILE` parameter of your [clinical params file](clinical.md).

WORD_MAP_FILE format
------------

|Filename|Column Number|Original value|New value |
|--------|-------------|--------------|----------|
|data.txt|5            |m             |Male      |
|data.txt|5            |f             |Female    |
|data.txt|12           |0             |No        |
|data.txt|5            |1             |Yes       |

Table, tab separated, txt file. It contains information about columns which are to be uploaded into tranSMART.

- `Filename`  This column determines the file where column is located
- `Category Code` Path which contains the file
- `Column Number` Index of the column from the data file, starting at 1
- `Data Label`  Label visible inside tranSMART after upload
- `Data Label Source` works for template column only (has `\` in data label column). Refers to the data label column where to get data for dynamic concept path generation. See [templates](templates.md) documentation for more details.
- `Control Vocab cd`  IGNORED skip if you don't need Concept Type Column
- `Concept Type`  Use this concept type instead of inferring it from the first row

Reserved keywords for Data Label:
- `SUBJ_ID` **(Mandatory)** Used to indicate the column that contains the subject IDs. Use exactly once per data file.
- `START_DATE` Observation level start date(s).
- `END_DATE` Observation level end date(s).
- `TRIAL_VISIT_LABEL` Name(s) of the trial visit(s) that observations belong to.
- `INSTANCE_NUM` Integer column used to distinguish repeated observations (i.e. identical time series data).
- `DATA_LABEL` Only used as Category Code placeholders. See [templates](templates.md) documentation.
- `VISIT_NAME` Only used as Category Code placeholders (max. 1 per Filename). See [templates](templates.md) documentation.
- `SITE_ID` Only used as Category Code placeholders (max. 1 per Filename). See [templates](templates.md) documentation.
- `\` Only used for variables containing placeholders in their Category Code. See [templates](templates.md) documentation.
- `OMIT` Used to indicate this variable should not be uploaded to tranSMART.
- `PATIENT_VISIT` **(Not yet implemented)** Integer variable used to link a subject's observations to a patient visit.

Allowed values for Concept type:
- `NUMERICAL` for numerical variables
- `CATEGORICAL` for categorical (text) variables 
