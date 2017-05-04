# Subject Sample Mapping

This is the mapping file format:

| `STUDY_ID` | `SITE_ID` | `SUBJECT_ID`     | `SAMPLE_CD` | `PLATFORM`   | `SAMPLE_TYPE` | `TISSUE_TYPE` | `TIME_POINT` | `CATEGORY_CD`                      | `SOURCE_CD` | `PATIENT_VISIT` **(not yet implemented)**|\<MODIFIER\> **(not yet implemented)**|
|------------|-----------|------------------|-------------|--------------|---------------|---------------|--------------|------------------------------------|-------------|-----------------|------------|
| GSE8581    |           | GSE8581GSM210005 | GSM210005   | GPL570_BOGUS | Tumor         | Lung          | Week1        | Biomarker_Data+PLATFORM+TISSUETYPE | STD         | 1               |            |

Currently the first row is skipped. It must be present, otherwise the first assay will be ignored.
**Not yet implemented:** after the next update the header will be read. This means the order of columns is no longer important, but column names will have to match the required format.

- `STUDY_ID` **Mandatory.** The ID of the study (has to match the `STUDY_ID` parameter in your [study.params](study.params)).
- `SITE_ID` **Deprecated.**
- `SUBJECT_ID` **Mandatory.** The subject id. Must match the one provided in the clinical data file(s).
- `SAMPLE_CD` **Mandatory.** The name of the assay (here synonymous with "sample").
- `PLATFORM` **Mandatory.** GPL ID of the corresponding platform. Platform must have already been loaded; must be the same for all rows. The value will be uppercased. Can be used as placeholder to replace `PLATFORM` in the `CATEGORY_CD`.
- `SAMPLE_TYPE` The type of sample Can be used to fill `sample_type` in `de_subject_sample_mapping`. It will also be used to replace
   the placeholder `SAMPLETYPE`in `CATEGORY_CD`.
- `TISSUE_TYPE` will be used to fill `tissue_type`. It will also be used to
  replace the placeholder `TISSUETYPE` and `ATTR1` (legacy) in `CATEGORY_CD`.
- `TIME_POINT`  will be used to fill `timepoint`. It will be used to
  replace the `TIMEPOINT` and `ATTR2` (legacy) placeholder in `CATEGORY_CD`. Optional.
- `CATGEORY_CD` will be used to form the concept path for the node to be
  created. Components of the path are separated with `+`. It can include several
  placeholders (see the descriptions of the other columns). In principle it can
  differ among the several assays, but that code path has never been tested.
- `SOURCE_CD` is ignored (must be present as a last column).
- `PATIENT_VISIT` **Not yet implemented.** Integer variable used to indicate this sample was collected during a patient visit provided in [patient visit mapping file](patient-visit-mapping.md).
