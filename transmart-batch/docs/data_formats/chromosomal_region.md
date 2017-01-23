# Chromosomal Regions Annotation

Below is description of the chromosomal region annotation input format.

| Column Name | Description |
---------|-------------------
| GPL_ID | **Mandatory** Platform ID. e.g. `GPL11154` |
| REGION_NAME | *Optional* The name of this region. It's usually the gene name. e.g. `LOC100132062` |
| CHROMOSOME | **Mandatory** The number of chromosome. Could contain a symbol. e.g. `X` chromosome |
| START_BP | **Mandatory** Start of the chromosomal region in base pairs. e.g. `326096` |
| END_BP | **Mandatory** End of the chromosomal region in base pairs. e.g. `328112` |
| NUM_PROBES | *Optional*  |
| CYTOBAND | *Optional* e.g. `1p36.33` |
| GENE_SYMBOL | **Mandatory** e.g. `TP53` |
| GENE_ID | **Mandatory** Id of the gene. Transmart uses Entrez Gene IDs. e.g. `2066` |
| ORGANISM | **Mandatory** Species. e.g. `Homo Sapiens` |
