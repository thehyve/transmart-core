# ACGH

## Platform Upload

Your platform configuration file has to have the `acgh_annotation.params` name.
The content of the params file is the same as for other HD data types.

For format of the platform data file see [chromosomal_region.md](chromosomal_region.md)

## ACGH Data Upload

Your must have `acgh.params` file. For possible parameter see [hd-params.md](hd-params.md).

Below is the expected file format for ACGH data input files.
First column always has to be region name. The rest of the columns are recognised by the name, not by position.
Replace `<sample_code>` with an actual sample code (e.g. `CACO2`).

| Column Name | Description |
--------------|--------------
| region_name | **Mandatory** The name of this region. Often it's a gene name. e.g. `WASH7P` |
| `<sample_code>`.flag | **Mandatory** `-2` - homologous loss, `-1` - loss `0` - normal, `1` - gain, `2` - amplification. |
| `<sample_code>`.chip | *Optional* log2 ratio of the measurement. |
| `<sample_code>`.segmented |  *Optional* Segmented log2 ratio. |
| `<sample_code>`.probhomloss | *Optional* Actual probability of homologous loss. |
| `<sample_code>`.probloss | *Optional* Actual probability of loss. |
| `<sample_code>`.probnorm |  *Optional* Actual probability of normal. |
| `<sample_code>`.probgain | *Optional* Actual probability of gain. |
| `<sample_code>`.probamp | *Optional* Actual probability of amplification. |


