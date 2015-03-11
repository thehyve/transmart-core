mRNA
==================

For historical reasons, the parameters file for mRNA data is called
`expression.params`.

Available parameters
--------------------

- `STUDY_ID` - Optional, defaults to the uppercased parent directory name of the
  `expression.params` file.
- `TOP_NODE` -- study top node, not the node for the mRNA data. Defaults to
  `\(Public|Private) Studies\<STUDY_ID>`.
- `SECURITY_REQUIRED` -- `Y` for yes, `N` for no. Default `N`.
- `DATA_FILE` -- prefer this to `DATA_FILE_PREFIX`. Mandatory (alternatively
  with `DATA_FILE_PREFIX`).
- `DATA_FILE_PREFIX` -- _deprecated_ because it doesn\'t behave like a prefix
  (unlike the original pipeline); use `DATA_FILE` instead.
- `DATA_TYPE` -- must be present; must be `R` (raw values)
- `LOG_BASE` -- optional, but if present must be `2`. It has no effect.
- `NODE_NAME` -- what to append to `TOP_NODE` for form the concept path of the
  mRNA node (before the part generated from `category_cd`).  Optional; default
  is `MRNA`. Cannot be omitted (see (JE-52)[1]). 
- `MAP_FILENAME` -- filename of the mapping file. Mandatory.
- `ALLOW_MISSING_ANNOTATIONS` -- `Y` for yes, `N` for no. Default `N`. Whether
  the job should be allowed to continue when the data set doesn't provide data
  for all the annotations (here probes).

Input files
-----------

This is the mapping file format:

| `STUDY_ID` | `SITE_ID` | `SUBJECT_ID`     | `SAMPLE_CD` | `PLATFORM`   | `TISSUETYPE` | `ATTRITBUTE_1` | `ATTRITBUTE_2` | `CATEGORY_CD`                 | `SOURCE_CD` |
|------------|-----------|------------------|-------------|--------------|--------------|----------------|----------------|-------------------------------|-------------|
| GSE8581    |           | GSE8581GSM210005 | GSM210005   | GPL570_BOGUS | Human        | Lung           |                | Biomarker_Data+PLATFORM+ATTR1 | STD         |

The first row is skipped. It must be present, otherwise the first assay will be
ignored.

- `STUDY_ID` is required, but must match the `STUDY_ID` parameter.
- `SITE_ID` is ignored.
- `SUBJECT_ID` is the subject id. Must match the one provided in the clinical
  data set.
- `SAMPLE_CD` is the name of the assay (here synonymous with "sample").
  Required.
- `PLATFORM` is the GPL id of the corresponding platform. Must be given; the
  platform must have already been loaded; must be the same for all rows; must be
  uppercase. It will be used to replace the `PLATFORM` placeholder in
  `CATEGORY_CD`.
- `TISSUETYPE` will be used to fill `sample_type` in
  `de_subject_sample_mapping`. It will *not* be used to fill `tissue_type`.
  Used to replace the `TISSUETYPE` placeholder in `CATEGORY_CD`.  Optional.
- `ATTRIBUTE_1` will be used to fill `tissue_type`. It will also be used to
  replace the placeholder `ATTR1` in `CATEGORY_CD`.
- `ATTRIBUTE_2` will be used to replace the `ATTR2` placeholder in
  `CATEGORY_CD`.  Optional.
- `CATGEORY_CD` will be used to form the concept path for the node to be
  created. Components of the path are separated with `+`. It can include several
  placeholders (see the descriptions of the other columns). In principle it can
  differ among the several assays, but that code path has never been tested.
- `SOURCE_CD` is ignored (must be present as a last column).


This is the data file format:

| `ID_REF`    | `GSM210005` | `GSM210006` | `GSM210007` |
|-------------|-------------|-------------|-------------|
| 1007\_s\_at | 0           | 179.15      |367.436      |

The first row must contain `ID_REF` as the first column, which will be the probe
name in the respective platform. These must match the probe names when the
platform was loaded. The order is not important, but the set of probes in the
data file must match exactly the set of probes for the platform.

The subsequent rows are the sample names, whose set must match that of those in
the `SAMPLE_CD` column in the mapping file. Again, the order is not important.

Empty cells are not allowed. Zeros, negative values and NaNs (input as U+FFFD)
*are* allowed and have the same effect among each other (see below).


Processing details
------------------

The data will be scanned twice. The first pass will only validate the data set.

n the second pass, the data will be inserted in the database. Zeros, negative
values and NaNs in the data file will *not* be inserted into the database.


The `zscore` column will be calculated as:

    clamp(-2.5, 2.5, (log_intensity - mean) / stdDev)

where `mean` and `stdDev` are values calculated over the `log_intensity` values
of all the values included in the same row (for the same probe). If the original
value (before the log calculation) was a non-positive number or `NaN`s, then it
will be excluded from the calculation of these statistics. This means each row
must have at least two positive numbers, otherwise the standard deviation cannot
be calculated. If the standard deviation is zero, the resulting zscore for all
the values in the row will be NaN. This NaN value *will be inserted* into the
database.

Finally, `clamp` is defined as

    double clamp(double lowerBound, double upperBound, double value) {
        Math.min(upperBound, Math.max(lowerBound, value))
    }

Clamping may not be a good idea, but it was implemented in this fashion in order
to match the value in the Kettle + stored procedure pipeline.

  [1]: https://jira.thehyve.nl/browse/JE-52

<!-- vim: tw=80 et ft=markdown spell:
-->
