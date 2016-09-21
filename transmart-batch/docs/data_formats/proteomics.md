Proteomics
==========

Annotation
----------

The parameters file should be named `proteomics_annotation.params`.

The annotations file has 7 columns.
3 of them contain gene related information.
The required order is `probesetID`, `uniprotID`,
`species`, `gpl_id`, `chromosome`, `start`, `end`.

Constraints:

- The only mandatory columns are `probesetID` and `uniprotID`.
- All gene region columns (`chromosome`, `start` and `end`) are required if at least one is provided. That is, either all or none of the gene information columns must be included.

**Note:** The proteomics platform upload relies on the proteomics dictionary to seek uniprot names for the given uniprot identifiers. Currently proteomics dictionary could be uploaded by means of transmart-data.
If there is no such protein information in the dictionary or the dictionary is not loaded at all the pipeline would fill in uniprot names with uniprot ids and warn user about this action.

Example file:

| probesetID | uniprotID | species      | gpl_id     | chromosome | start     | end       |
|------------|-----------|--------------|------------|------------|-----------|-----------|
| 5060       | Q9UBS8    | Homo Sapiens | PROT_ANNOT | 5          | 141348450 | 141369856 |
| 2739       | P62805    | Homo Sapiens | PROT_ANNOT | 6          | 26021906  | 26022278  |
| 2260       | P35573    | Homo Sapiens | PROT_ANNOT | 1          | 100316044 | 100389579 |
| 611        | E9PC15    | Homo Sapiens | PROT_ANNOT | 7          | 141251077 | 141354209 |
| 2959       | Q09161    | Homo Sapiens | PROT_ANNOT | 9          | 100395704 | 100436029 |
| 1860       | P11021    | Homo Sapiens | PROT_ANNOT | 9          | 127997126 | 128003666 |
| 2243       | P34932    | Homo Sapiens | PROT_ANNOT | 5          | 132387661 | 132440709 |
| 4041       | Q8WX92    | Homo Sapiens | PROT_ANNOT | 9          | 140149758 | 140168000 |


Refer to the [general annotations documentation](annotations.md) for general
information about annotation loading jobs, including their parameters.

Subject-Sample Mapping
----------------------

Here is a subject-sample mapping example file:

| trial_name | site_id | subject_id | sample_cd               | platform   | sample_type | tissue_type | time_point | cat_cd                                                                                                                   | src_cd |
|------------|---------|------------|-------------------------|------------|-------------|-------------|------------|--------------------------------------------------------------------------------------------------------------------------|--------|
| CLUC       |         | CACO2      | LFQ.intensity.CACO2_1   | PROT_ANNOT | LFQ-1       | Colon       | Week1      | Molecular profiling+High-throughput molecular profiling+Expression (protein)+LC-MS-MS+Protein level+SAMPLETYPE+MZ ratios | STD    |
| CLUC       |         | CACO2      | LFQ.intensity.CACO2_2   | PROT_ANNOT | LFQ-2       | Colon       | Week1      | Molecular profiling+High-throughput molecular profiling+Expression (protein)+LC-MS-MS+Protein level+SAMPLETYPE+MZ ratios | STD    |
| CLUC       |         | COLO205    | LFQ.intensity.COLO205_1 | PROT_ANNOT | LFQ-1       | Colon       | Week1      | Molecular profiling+High-throughput molecular profiling+Expression (protein)+LC-MS-MS+Protein level+SAMPLETYPE+MZ ratios | STD    |
| CLUC       |         | COLO205    | LFQ.intensity.COLO205_2 | PROT_ANNOT | LFQ-2       | Colon       | Week1      | Molecular profiling+High-throughput molecular profiling+Expression (protein)+LC-MS-MS+Protein level+SAMPLETYPE+MZ ratios | STD    |


Data
----

The parameters file should be named `proteomics.params`.
For the content of this file see [the HD data parameters](hd-params.md) and [the study-specific parameters](study-params.md).

Example data file:

| ID_REF | LFQ.intensity.CACO2_1 | LFQ.intensity.CACO2_2 | LFQ.intensity.COLO205_1 | LFQ.intensity.COLO205_2 |
|--------|-----------------------|-----------------------|-------------------------|-------------------------|
| 1860   | 9089400000            | 8792800000            | 8949100000              | 7252500000              |
| 2243   | 4997100000            | 5527800000            | 4280900000              | 4196200000              |
| 2739   | 2160700000            | 2090600000            | 30589000000             | 4188200000              |
| 2959   | 395180000             | 468840000             | 349410000               | 494790000               |
| 2260   | 101860000             | 84585000              | 93405000                | 101120000               |
| 4041   | 52779000              | 62863000              | 53180000                | 72288000                |
| 611    | 37153000              | 30627000              | 87144000                | 42039000                |
| 5060   | 0                     | 26186000              | 0                       | 0                       |

Format expectations:

- The first column has to be named `ID_REF` and contain a row identifier. The row identifier has to match the `probesetID` from the related platform.
- The rest of the columns have to be intensities. There must not be other columns in the file.
- The header names for the intensities columns have to match the `sample_cd` values of the subject sample mapping file.

[Read more on how HD data are processed.](../hd-data-processing-details.md)

<!-- vim: tw=80 et ft=markdown spell:
-->
