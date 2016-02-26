# ACGH

## Platform Upload

Your platform configuration file has to have the `acgh_annotation.params` name.
The content of the params file is the same as for other HD data types.

For format of the platform data file see [chromosomal_region.md](chromosomal_region.md)

## ACGH Data Upload

Your must have `acgh.params` file. For possible parameter see [hd-params.md](hd-params.md).

Below is the expected file format for ACGH data input files (column positions are importent, column names are not).

| Column Name | Description |
--------------|--------------
| REGION_NAME | **Mandatory** The name of this region. Often it's a gene name. e.g. `WASH7P` |
| SAMPLE_ID | **Mandatory** Sample id. e.g. `CACO2` |
| FLAG | **Mandatory** `-2` - homologous loss, `-1` - loss `0` - normal, `1` - gain, `2` - amplification. |
| CHIP | *Optional* log2 ratio of the measurement. |
| SEGMENTED |  *Optional* Segmented log2 ratio. |
| PROBHOMLOSS | *Optional* Actual probability of homologous loss. |
| PROBLOSS | *Optional* Actual probability of loss. |
| PROBNORM |  *Optional* Actual probability of normal. |
| PROBGAIN | *Optional* Actual probability of gain. |
| PROBAMP | *Optional* Actual probability of amplification. |


