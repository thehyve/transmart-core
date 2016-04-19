mRNA
==================

For historical reasons, the parameters file for mRNA data is called
`expression.params`.
For the content of this file see [the HD data parameters](hd-params.md) and [the study-specific parameters](study-params.md).

Annotation
----------

Parameters file name has to be `mrna_annotation.params`.
`annotation.params` name is also supported as a legacy.

**Attention:** The data file referred by `ANNOTATIONS_FILE` field  of the `mrna_annotation.params` file is expected to have a header (**first line is skipped**).
While the data file referred from `annotation.params` **must not have a header**.
Such distinction made for backward compatibility reasons: old platform files did not have a header.

The annotation data file has to have following columns:

|   Column Name   |            Description             |
|-----------------|------------------------------------|
| GPL_ID          | Platform id (e.g. `GPL570`)        |
| PROBE_NAME      | Probe name (e.g. `1007_s_at`)      |
| GENES           | Gene name (e.g. `DDR1`)            |
| ENTREZ_IDS      | Entrez gene id (e.g. `780`)        |
| ORGANISM        | Species name (e.g. `Homo Sapiens`) |


Data
----

### Subject Sample Mapping

See [subject-sample-mapping.md](subject-sample-mapping.md)


### Expression Data

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

[Read more on how HD data are processed.](hd-data-processing-details.md)

  [1]: https://jira.thehyve.nl/browse/JE-52

<!-- vim: tw=80 et ft=markdown spell:
-->
