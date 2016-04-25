High dimensional data parameters
--------------------------------

- `DATA_FILE` **Mandatory** (alternatively
  with `DATA_FILE_PREFIX`). _Prefer this to_ `DATA_FILE_PREFIX`. Points to the HD data file.
- `DATA_FILE_PREFIX` ___deprecated___ because it doesn't behave like a prefix
  (unlike the original pipeline); use `DATA_FILE` instead.
- `DATA_TYPE` **Mandatory**; must be `R` (raw values). Other types are not supported yet.
- `LOG_BASE` _Default:_ `2`. If present must be `2`. The log base for calculating log values.
- `MAP_FILENAME` **Mandatory**. Filename of the mapping file.
- `ALLOW_MISSING_ANNOTATIONS` _Default:_ `N`. `Y` for yes, `N` for no. Whether
  the job should be allowed to continue when the data set doesn't provide data
  for all the annotations (here probes).
- `SKIP_UNMAPPED_DATA` _Default:_ `N`. If `Y` then it ignores data points that have no subject mapping. Otherwise (`N`) gives an error for such data points.
- `ZERO_MEANS_NO_INFO` _Default:_ `N`. If `Y` then the rows with raw values equal 0 would be filtered out. Otherwise (`N`) they will be inserted to the database.
    The flag applies to most of HD data types. It does not effect CNV (ACGH) data. For RnaSeq data the chcek on zeros happens based on normalized read count.
