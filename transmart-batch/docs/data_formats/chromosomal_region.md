# Chromosomal Regions Annotation

Below is description of the chromosomal region annotation input format.

| Column Name | Description |
---------|-------------------
| GPL_ID | **(Mandatory)** Platform ID (e.g. `GPL11154`). Has to be identical for all rows. |
| REGION_NAME | **(Mandatory)** The name of the region. Usually a gene name (e.g. `LOC100132062`) or region indicator (e.g. chr4:1215324-1942886). Has to be unique. |
| CHROMOSOME | **(Mandatory for [CNV](cnv.md))** The chromosome number. Human allosomes should be indicated with `X` or `Y`. |
| START_BP | **(Mandatory for [CNV](cnv.md))** Start of the chromosomal region in base pairs (e.g. `326096`) |
| END_BP | **(Mandatory for [CNV](cnv.md))** End of the chromosomal region in base pairs (e.g. `328112`) |
| NUM_PROBES | If applicable, the number of probes that are present in the chromosomal region.  |
| CYTOBAND | The name of the cytoband the chromosomal region is located on (e.g. `1p36.33`) |
| GENE_SYMBOL | The HGNC gene symbol this chromosomal region encompasses (e.g. `AURKA`). |
| GENE_ID | ID of the gene. Transmart uses Entrez Gene IDs (e.g. `2066`). |
| ORGANISM | **(Mandatory)** Scientific name of the species the platform is based on (e.g. `Homo sapiens`). |
