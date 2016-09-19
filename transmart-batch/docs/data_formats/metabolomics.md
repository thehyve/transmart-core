Metabolomics
============

Annotation
----------

The parameters file should be named `metabolomics_annotation.params`.

The annotations file has 4 columns. These can be in any order, but their names
need to be correct. The recommended order is `BIOCHEMICAL`, `SUPER_PATHWAY`,
`SUB_PATHWAY` and `HMDB_ID`. Constraints:

- The only mandatory column is `BIOCHEMICAL`.
- The column `SUB_PATHWAY` is required if `SUPER_PATHWAY` is provided.
- If a super-pathway is included in more than one row, the corresponding
  sub-pathway must be the same in all the cases.
- The columns `BIOCHEMICAL` and `HMDB_ID` must be independently unique. (Note:
  the database schema supports associating one biochemical with multiple
  sub-pathways; this is currently not supported in the job).

Example file:

| BIOCHEMICAL                            | SUPER\_PATHWAY  | SUB\_PATHWAY                                 | HMDB\_ID  |
|----------------------------------------|-----------------|----------------------------------------------|-----------|
| mevalonic acid                         | Carboxylic acid | Mevalonic acid pathway                       | HMDB0TEST |
| 5-isopentenyl pyrophosphoric acid      | Phosphoric acid | Cholesterol biosynthesis                     |           |
| 3,3-dimethyl allyl pyrophosphoric acid | Phosphoric acid | Cholesterol biosynthesis                     |           |
| xylitol                                | Carbohydrate    | Nucleotide sugars; pentose metabolism        | HMDB00568 |
| farnesyl pyrophosphate                 | Phosphoric acid | Cholesterol biosynthesis; Squalene synthesis |           |
| presqualene diphosphate                | Phosphoric acid | Cholesterol biosynthesis; Squalene synthesis |           |
| bogus no super                         |                 | Sub pathway without super                    |           |
| bogus no sub/super                     |                 |                                              |           |

Refer to the [general annotations documentation](annotations.md) for general
information about annotation loading jobs, including their parameters.

Data
----

The parameters file should be named `metabolomics.params`.
For the content of this file see [the HD data parameters](hd-params.md) and [the study-specific parameters](study-params.md).

[Read more on how HD data are processed.](../hd-data-processing-details.md)

<!-- vim: tw=80 et ft=markdown spell:
-->
