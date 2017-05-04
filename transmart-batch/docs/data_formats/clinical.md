Clinical Data
================

Clinical data is meant for all kind of measurements not falling into other
categories. It can be data from questionnaires, physical body measurements or
socio-economic info about the patients.


Parameters
------------
- `COLUMN_MAP_FILE` **(Mandatory)** Points to the column file. See below for format.
- `WORD_MAP_FILE` Points to the file with dictionary to be used.
- `XTRIAL_FILE` Points to the [cross study concepts file](xtrial.md).
- `TAGS_FILE` Points to the [concepts tags file](tags.md).
- `ONTOLOGY_MAP_FILE` Points to the [ontology mapping file](ontology-mapping.md).
- `TRIAL_VISIT_MAP_FILE` **(Not yet implemented)** Points to the [trial visit mapping file](trial-visit-mapping.md).
- `PATIENT_VISIT_MAP_FILE` **(Not yet implemented)** Points to the [patient visit mapping file](patient-visit-mapping.md).


COLUMN_MAP_FILE format
------------

|Filename|Category Code|Column Number|Data Label|Data Label Source|Control Vocab Cd|Concept Type |
|--------|-------------|-------------|----------|-----------------|----------------|-------------|
|data.txt|folder       |1            |Weight    |                 |                |NUMERICAL    |

Table, tab separated, txt file. It contains information about columns which are to be uploaded into tranSMART.

- `Filename`  The name of the file that contains the variable being described
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
