High dimensional data parameters
--------------------------------

- `DATA_FILE` **Mandatory** (alternatively
  with `DATA_FILE_PREFIX`). _Prefer this to_ `DATA_FILE_PREFIX`. Points to the HD data file.
- `DATA_FILE_PREFIX` ___deprecated___ because it doesn't behave like a prefix
  (unlike the original pipeline); use `DATA_FILE` instead.
- `DATA_TYPE` **Mandatory**; must be `R` (raw values). Other types are not supported yet.
- `LOG_BASE` _Default:_ `2`. If present must be `2`. The log base for calculating log values.
- `NODE_NAME` _Default:_ `<HD data type name>`. What to append to `TOP_NODE` for form the concept path of the
  HD node (before the part generated from `category_cd`). Cannot be omitted (see (JE-52)[1]).
- `MAP_FILENAME` **Mandatory**. Filename of the mapping file.
- `ALLOW_MISSING_ANNOTATIONS` _Default:_ `N`. `Y` for yes, `N` for no. Whether
  the job should be allowed to continue when the data set doesn't provide data
  for all the annotations (here probes).
