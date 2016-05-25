miRNA
=====

The parameters file for miRNA data is called `mirna.params`.
For the content of this file see [the HD data parameters](hd-params.md) and [the study-specific parameters](study-params.md).

Annotation
----------

Parameters file name has to be `mirna_annotation.params`.

The annotation data file has to have following columns:

|   Column Name   |               Description                |
|-----------------|------------------------------------------|
| ID_REF          | Any id (e.g. `1`) just to join with data |
| MIRNA_ID        | miRNA identifier (e.g. `hsa-mir-302d`)   |

**Note:** Do not forget to upload miRNA dictionary with transmart-data:
`make -C data/{postgres|oracle}/ load_mirna_dictionary`

`mirna_id` values would be lower cased.

Data
----

### Subject Sample Mapping

See [subject-sample-mapping.md](subject-sample-mapping.md)

### Expression Data

This is the data file format:

| `ID_REF` | `GSM210005` | `GSM210006` | `GSM210007` |
|----------|-------------|-------------|-------------|
|     1    | 0           | 179.15      |367.436      |

The first row must contain `ID_REF` as the first column, which will be the id
name in the respective platform. 

The subsequent rows are the sample names, whose set must match that of those in
the `SAMPLE_CD` column in the mapping file. The order is not important.

Empty cells are not allowed. Zeros, negative values and NaNs (input as U+FFFD)
*are* allowed and have the same effect among each other (see below).

[Read more on how HD data are processed.](hd-data-processing-details.md)

  [1]: https://jira.thehyve.nl/browse/JE-52

<!-- vim: tw=80 et ft=markdown spell:
-->
