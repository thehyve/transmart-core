GWAS
====

Available Parameters
--------------------

- `META_DATA_FILE` **Mandatory**. The path to the _metadata file_
  (see below). Can be a path relative to the `gwas` folder next to the
  parameters file or an absolute file.
- `DATA_LOCATION` _Default_: parent directory of the metadata file. The
  directory that functions as the path against which the data files
  referenced in the metadata file are to be found.
- `HG_VERSION` _Default_: 19. Either 18 or 19. The reference assembly to use.

Metadata file
-------------
A TSV that functions as an index for several analyses in the same study. This
pipeline uses the following mandatory columns:

* `STUDY` **Mandatory**. The name of the study (max: 50 characters).
* `DATA_TYPE` **Mandatory**. Must be `GWAS`.
* `ANALYSIS_NAME` **Mandatory**. A short name for the analysis. It should
  **not** include the string `"test"` (or any variant where one or more of the
  characters are uppercase), otherwise the analysis will not show up in GWAVA.
  Max: 500 characters. The tuple `(STUDY, ANALYSIS_NAME)` must be unique.
* `INPUT_FILE`. **Mandatory**. The name of the file containing the analysis'
  data.

Other columns can provide more meta data about the analysis. The full list can
be seen by looking at the
[example file](../../studies/MAGIC/gwas/MagicDataSet.tsv).

Analyses Data File
------------------

An example analysis data file can be found
[here](../../studies/MAGIC/gwas/mod_MAGIC_FastingGlucose.tsv). No column is
mandatory, except for `RS_ID`. However, the `P_VALUE` should also be given for
most functionality to work. Alleles may contain only one character.

<!-- vim: tw=80 et ft=markdown spell:
-->
