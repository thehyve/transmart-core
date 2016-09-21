# RNASeq

## Platform Upload

Your platform configuration file has to have the `rnaseq_annotation.params` name.
The content of the params file is the same as for other HD data types.

For format of the platform data file see [chromosomal_region.md](chromosomal_region.md)

## RNASeq Data Upload

Your must have `rnaseq.params` file. For possible parameter see [hd-params.md](hd-params.md).

Below is the expected file format for RNASeq data input files.
First column always has to be region name. The rest of the columns are recognised by the name, not by position.
Replace `<sample_code>` with an actual sample code (e.g. `CACO2`).

| Column Name | Description |
--------------|--------------
| REGION_NAME | **Mandatory** The name of this region. Often it's a gene name. e.g. `WASH7P` |
| `<sample_code>`.readcount | **Mandatory** Actual measurement. |
| `<sample_code>`.normalizedreadcount | *Optional* Normalized readcount. (e.g. RPKM) |


