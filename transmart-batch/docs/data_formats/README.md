# Loading tranSMART data

## For all data types
* Parameters for all data types
  * [study-params.md](study-params.md) - Parameters that are supported for all data types.
* Metadata
  * [tags.md](tags.md) - Loading study, concept, patient metadata and links to source data per concept.

## Low-dimensional data
* Clinical data
  * [clinical.md](clinical.md) - Loading numerical and categorical low-dimensional data from clinical, non-high throughput molecular profiling, derived imaging data, biobanking data or links to source data per patient.
    * [templates.md](templates.md) - Using templates in the clinical data paths.
    * [xtrial.md](xtrial.md) - Uploading across trial clinical data.

## High-dimensional data
* General high-dimensional data processing
  * [hd-params.md](hd-params.md) - Parameters that are supported for all high-dimensional data types.
  * [chromosomal_region.md](chromosomal_region.md) - Tabular file structure for loading chromosomal regions.
  * [subject-sample-mapping.md](subject-sample-mapping.md) - Tabular file structure for loading subject sample mappings for HD data.
* mRNA gene expression data
  * [expression.md](expression.md) - Loading microarray gene expression data.
  * *under development* - Loading readcounts and normalized readcounts data for mRNAseq and miRNAseq.
* Copy Number Variation data
  * [cnv.md](cnv.md) - Loading CNV data from Array CGH (comparative genomic hybridisation), SNP Array, DNA-Seq, etc.
* Small Genomic Variants
  * *not yet implemented* - Loading small genomic variants (SNP, indel in VCF format) from RNAseq or DNAseq.
* Proteomics data
  * [proteomics.md](proteomics.md) - Loading protein mass spectrometry data as peptide or protein quantities.
* RnaSeq data
  * [rnaseq.md](rnaseq.md) - Loading gene region RNASeq data as read counts and normalized read counts.
* Metabolomics data
  * [metabolomics.md](metabolomics.md) - Loading metabolite quantities.
* GWAS data
  * [gwas.md](gwas.md) - Loading Genome Wide Association Study data.

# Other
* Unloading tranSMART data
  * [backout.md](backout.md) - Deleting data from tranSMART.
* Loading I2B2 data
  * [i2b2.md](i2b2.md) - Loading data to I2B2 with transmart-batch.
