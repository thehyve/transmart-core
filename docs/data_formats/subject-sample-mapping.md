# Subject Sample Mapping

This is the mapping file format:

| `STUDY_ID` | `SITE_ID` | `SUBJECT_ID`     | `SAMPLE_CD` | `PLATFORM`   | `SAMPLE_TYPE`| `TISSUE_TYPE`  | `TIME_POINT`   | `CATEGORY_CD`                      | `SOURCE_CD` |
|------------|-----------|------------------|-------------|--------------|--------------|----------------|----------------|------------------------------------|-------------|
| GSE8581    |           | GSE8581GSM210005 | GSM210005   | GPL570_BOGUS | Human        | Lung           | Week1          | Biomarker_Data+PLATFORM+TISSUETYPE | STD         |

The first row is skipped. It must be present, otherwise the first assay will be
ignored.

- `STUDY_ID` is required, but must match the `STUDY_ID` parameter.
- `SITE_ID` is ignored.
- `SUBJECT_ID` is the subject id. Must match the one provided in the clinical
  data set.
- `SAMPLE_CD` is the name of the assay (here synonymous with "sample").
  Required.
- `PLATFORM` is the GPL id of the corresponding platform. Must be given; the
  platform must have already been loaded; must be the same for all rows. The value will be uppercased.
  It will be used to replace the `PLATFORM` placeholder in
  `CATEGORY_CD`.
- `SAMPLE_TYPE` will be used to fill `sample_type` in
  `de_subject_sample_mapping`. It will also be used to replace
   the placeholder `SAMPLETYPE`in `CATEGORY_CD`. Optional.
- `TISSUE_TYPE` will be used to fill `tissue_type`. It will also be used to
  replace the placeholder `TISSUETYPE` and `ATTR1` (legacy) in `CATEGORY_CD`.
- `TIME_POINT`  will be used to fill `timepoint`. It will be used to
  replace the `TIMEPOINT` and `ATTR2` (legacy) placeholder in `CATEGORY_CD`. Optional.
- `CATGEORY_CD` will be used to form the concept path for the node to be
  created. Components of the path are separated with `+`. It can include several
  placeholders (see the descriptions of the other columns). In principle it can
  differ among the several assays, but that code path has never been tested.
- `SOURCE_CD` is ignored (must be present as a last column).
