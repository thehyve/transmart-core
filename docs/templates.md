Template category code for producing dynamic concept paths that based on data
=============================================================================

Below is an example on how you could use template columns.

Column mapping file
-------------------

|Filename|Category Code                          |Column Number|Data Label|Data Label Source|
|--------|---------------------------------------|-------------|----------|-----------------|
|data.txt|                                       |   4         |DATA_LABEL|                 |
|data.txt|                                       |   5         |DATA_LABEL|                 |
|data.txt|                                       |   6         |VISIT_NAME|                 |
|data.txt|                                       |   7         |SITE_ID   |                 |
|data.txt|Characteristics+DATALABEL+VISITNAME+BMI|   8         |\         |   5             |
|data.txt|Characteristics+SITEID                 |   9         |\         |   4             |

- `\` in `Data Label` column indicate that this column is a template.
- `Category Code` of a template could contain placeholders. There are 3 placeholders supported so far: `DATALABEL`,
`VISITNAME`, `SITEID`.  Note that unlike corespondant column data labels placeholders do not have underscore in it.
We have to support exactly these names for placeholders for legacy reasons.
- `Data Label Source` column points to a column with a data label. Unlike `VISIT_NAME` and `SITE_ID`
there could be multiple `DATA_LABEL` column declaration for one file.

Consider below row from the `data.txt`.

|...| 4             | 5       | 6      | 7          | 8 | 9         |
|---|---------------|---------|--------|------------|---|-----------|
|...|Side effects   |Treatment|Baseline|FOO Hospital|26 |No mutation|

- Template column `8` from the column mapping file would produce following concept path:
`...\Characteristics\Treatment\Baseline\BMI\`.
- Template column `9` from the column mapping file would produce following concept path:
`...\Characteristics\FOO Hospital\Side effects\`. Note that data label, if not specified, is added to the end.
