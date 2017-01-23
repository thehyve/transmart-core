Concept Path Templating Based on Data
=============================================================================

Some columns of the data file can have their cell values be used not to insert
facts but rather to build concept paths for the facts in the other columns of
the same data file row. This is done through two separate types of entries in
the column mapping file. One of these are entries that represent facts and have
a `Category Code` which includes certain placeholders and the other ones are
entries that that point to the columns in the data file where the values for
these placeholders should be found.

The three placeholders are `DATALABEL`, `VISITNAME`, and `SITEID`.  More than
one column of the same data file may be associated with the `DATALABEL`
placeholder. Therefore, whenever the `DATALABEL` placeholder is referenced in
the `Category Code` template, this reference must be disambiguated.

Below is an example and some further explanation.

Column mapping file
-------------------

|Filename|Category Code                          |Column Number|Data Label |Data Label Source|
|--------|---------------------------------------|-------------|-----------|-----------------|
|data.txt|                                       |   4         |DATA\_LABEL|                 |
|data.txt|                                       |   5         |DATA\_LABEL|                 |
|data.txt|                                       |   6         |VISIT\_NAME|                 |
|data.txt|                                       |   7         |SITE\_ID   |                 |
|data.txt|Characteristics+DATALABEL+VISITNAME+BMI|   8         |\          |   5             |
|data.txt|Characteristics+SITEID                 |   9         |\          |   4             |

- Entries that define placeholder values have the respective placeholder name in
  the `Data Label` column. The `Category Code` should be empty for these
  entries, as values for the respective column in the data file will not be
  inserted as facts.
- **IMPORTANT**: `\` in the `Data Label` column must be present when placeholders are used
  in the `Category Code`. It indicates that the `Category Code` column contains a template.
- There are 3 placeholders supported so far: `DATALABEL`, `VISITNAME`, `SITEID`.
  Note that, unlike their corespondent `Data Label` values, placeholders do not
  have an underscore.  We have to support these exact names for placeholders for
  legacy reasons.
- The `Data Label Source` column points to a data file column associated with
  `DATA_LABEL` placeholder values. Unlike `VISIT_NAME` and `SITE_ID`, there can
  be multiple `DATA_LABEL` declarations for the same file.

Consider the following row from the data file `data.txt`.

|...| 4             | 5       | 6      | 7          | 8 | 9         |
|---|---------------|---------|--------|------------|---|-----------|
|...|Side effects   |Treatment|Baseline|FOO Hospital|26 |No mutation|

- Template column `8` from the column mapping file will produce the following
  concept path:  
  `...\Characteristics\Treatment\Baseline\BMI\`.
  Instead of adding the value of the datalabel to the `Data label` column you
  can add it to the `Category Code` by appending a + with the data label.
- Template column `9` from the column mapping file would produce the following
  concept path:  
  `...\Characteristics\FOO Hospital\Side effects\`. Note that the data label,
  if not specified in the `Category Code` template, is added to the end.

For an example study please look at the [TreeStudy](https://github.com/thehyve/transmart-batch/tree/master/studies).
