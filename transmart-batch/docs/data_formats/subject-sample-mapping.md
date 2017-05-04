# Subject Sample Mapping

This is the mapping file format:

| STUDY_ID | SITE_ID | SUBJECT_ID     | SAMPLE_CD | PLATFORM   | SAMPLE_TYPE | TISSUE_TYPE | TIME_POINT | CATEGORY_CD                      | SOURCE_CD | PATIENT_VISIT **(not yet implemented)**|\<MODIFIER\> **(not yet implemented)**|
|------------|-----------|------------------|-------------|--------------|---------------|---------------|--------------|------------------------------------|-------------|-----------------|------------|
| GSE8581    |           | GSE8581GSM210005 | GSM210005   | GPL570_BOGUS | Tumor         | Lung          | Week1        | Biomarker_Data+PLATFORM+TISSUETYPE | STD         | 1               |            |

Currently the first row is skipped. It must be present, otherwise the first assay will be ignored.
**Not yet implemented:** after the next update the header will be read. This means the order of columns is no longer important, but column names will have to match the required format.

- `STUDY_ID` **(Mandatory)** The ID of the study (has to match the `STUDY_ID` parameter in your [study.params](study.params)).
- `SITE_ID` **(Deprecated)** Column must be present, but will be ignored.
- `SUBJECT_ID` **(Mandatory)** The subject id. Must match the one provided in the clinical data file(s).
- `SAMPLE_CD` **(Mandatory)** The name of the assay (here synonymous with "sample").
- `PLATFORM` **(Mandatory)** GPL ID of the corresponding platform. Platform must have already been loaded; must be the same for all rows. The value will be uppercased. May be used as placeholder to replace `PLATFORM` in the `CATEGORY_CD`.
- `SAMPLE_TYPE` Study context dependent indicator of the type of sample. Usually a genetic or phenotypic sample state. May be used as a placeholder to replace `SAMPLETYPE` in the `CATEGORY_CD`.
- `TISSUE_TYPE` The type of tissue or part of the body from which the sample originates. May be used as a placeholder to replace `TISSUETYPE` and `ATTR1` (legacy) in the `CATEGORY_CD`.
- `TIME_POINT` Timepoint indicator at which the sample was isolated. May be used as a placeholder to replace `TIMEPOINT` and `ATTR2` (legacy) in the `CATEGORY_CD`.
- `CATGEORY_CD` **(Mandatory)** The concept path of the node to be created. Components of the path are separated by a `+`. It may include several placeholders (see the descriptions of the other columns). Usually the same for all rows.
- `SOURCE_CD` **(Deprecated)** Column must be present, but will be ignored.
- `PATIENT_VISIT` **(Not yet implemented)** Integer variable used to indicate this sample was collected during a patient visit provided in [patient visit mapping file](patient-visit-mapping.md).
- `<MODIFIER>` **(Not yet implemented)** Additional columns may be added for each modifier you would like to use. The column name should be named `CAT_MOD:` (for categorical modifiers) or `NUM_MOD:` (for numerical modifiers) followed by the name of the modifier (e.g. `CAT_MOD:Brusatol dosage (µg)`)
