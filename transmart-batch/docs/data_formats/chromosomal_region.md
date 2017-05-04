# Chromosomal Regions Annotation

Below is description of the chromosomal region annotation input format.

| Column Name | Description |
---------|-------------------
| GPL_ID | **(Mandatory)** Platform ID. (e.g. `GPL11154`) |
| REGION_NAME | The name of the region. Usually a gene name (e.g. `LOC100132062`) or region indicator (e.g. chr4:1215324-1942886) |
| CHROMOSOME | **(Mandatory for XXXX)** The chromosome number. Human allosomes should be indicated with `X` or `Y`. |
| START_BP | **(Mandatory for XXXX)** Start of the chromosomal region in base pairs (e.g. `326096`) |
| END_BP | **(Mandatory for XXXX)** End of the chromosomal region in base pairs (e.g. `328112`) |
| NUM_PROBES | If applicable, the number of probes that are present in the chromosomal region.  |
| CYTOBAND | The name of the cytoband the chromosomal region is located on (e.g. `1p36.33`) |
| GENE_SYMBOL | **(Mandatory for XXXX)** The HGNC gene symbol this chromosomal region encompasses (e.g. `AURKA`). |
| GENE_ID | **(Mandatory)** ID of the gene. Transmart uses Entrez Gene IDs (e.g. `2066`). |
| ORGANISM | **(Mandatory)** Species (e.g. `Homo sapiens`). |
