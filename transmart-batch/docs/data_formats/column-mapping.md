Column Mapping
================

The column mapping file is a mandatory part of uploading a study to tranSMART. It contains information on which columns from your [data file(s)](clinical_data_file.md) should be uploaded, what their variable names should be, and where they should end up in the study's tree structure. The name of your column mapping file has to be provided in your [clinical.params](clinical.md) file.


COLUMN_MAP_FILE format
------------

|Filename|Category Code|Column Number|Data Label|Data Label Source|Control Vocab Cd|Concept Type |
|--------|-------------|-------------|----------|-----------------|----------------|-------------|
|data.txt|folder       |1            |Weight    |                 |                |NUMERICAL    |

Table, tab separated, txt file. The header is mandatory, but is not interpreted.

- `Filename` **(Mandatory)** The name of the data file that contains the variable to be mapped.
- `Category Code` **(Mandatory if `data label` not in reserved keywords)** The concept path of the node to be created. The `Category Code` therefore determines the tree structure of your study. Nodes of the path should be separated by a `+`.
- `Column Number` Index of the column from the data file, starting at 1.
- `Data Label`  Variable label visible in tranSMART after upload (the leaf node).
- `Data Label Source` works for template column only (has `\` in data label column). Refers to the data label column where to get data for dynamic concept path generation. See [templates](templates.md) documentation for more details.
- `Control Vocab cd`  **(IGNORED)** Skip if you don't need Concept Type Column.
- `Concept Type`  Use concept type to manually define the type of variable, instead of inferring it from the first row of the data.

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
- `NUMERICAL` for numerical variables.
- `CATEGORICAL` for categorical (text) variables.
