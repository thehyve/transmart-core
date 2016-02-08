# TranSMART RNASeq Support

There are two different RNASeq implementations in TranSMART.

## 1. TranSMART RNASeq Read Count Support (TraIT)

### 1.1. ETL Pipeline

#### 1.1.1. Step 1. Load chromosomal regions data.

Exactly the same resources (scripts, db tables, db stored procedure) are used for upploading platform for ACGH data.

Chromosomal regions input file is tabular file with such columns (order matters):

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

Above file gets uploaded to a temporarily table with the similar structure `tm_lz.lt_chromosomal_region`.

To push the data to production tables `tm_cz.i2b2_load_chrom_region` stored procedure is used.

Finally the data appear in two production tables. One row for platform definition in `deapp.de_gpl_info` table and chromosomal regions data copy in `deapp.de_chromosomal_region` table.

NOTE: Using of the temporary table and the stored procedure is questionable. It looks like we would not loose anything by uploading data directly to production zone.

#### 1.1.2. Step 2. Load RNASeq data.

The data upload requires two files: [subject-sample mapping file](https://github.com/thehyve/transmart-batch/blob/master/docs/proteomics.md#subject-sample-mapping) (similar file structure for all HD data types) and actual RNASeq data file.
RNASeq data file is tsv file with following collumns (order matters):

| Column Name | Description |
--------------|--------------
| GPL_ID | **Mandatory** Platform ID. e.g. `GPL11154` |
| GENE_SYMBOL | **Mandatory** The name of this gene. e.g. `WASH7P` |
| SAMPLE | **Mandatory** Sample id. e.g. `CACO2` |
| READCOUNT | **Mandatory** Actual measurement. |
| NORM_READCOUNT | *Optional* Normalized readcount. (e.g. RPKM) |

The data gets uploaded following usual schama: input file `--bash script-->` temp. tables `--stored procedure-->` prod. tables.
The subject-sample mapping file gets upploaded to `tm_lz.lt_src_mrna_subj_samp_map`.
The data file goes to `tm_lz.lt_src_rnaseq_data`.
`tm_cz.i2b2_process_rnaseq_data` stored procedure pumps data from temp. tables to production one: `deapp.de_subject_rnaseq_data`. The production table has `log_normalized_readcount` and `zscore` fields that gets calculated by stored procedure if norm. readcount is provided.

#### 1.1.3. Oracle version of the RNASeq ETL pipeline.

You could find all mentioned resources in oracle database as well. Although we anticipate that pipeline would need several further fixes to be able to start working. It'd be better to rewrite this pipeline and make it part of [transmart-batch](https://github.com/thehyve/transmart-batch).

### 1.2. TranSMART Analyses that works with RNASeq Read Count Data.

It looks like there only one analyses that works with RNASeq Read Count data at the moment. It's `Group Test for RNASeq`.

# 2. RNASeq (Sanofi). Float numbers. Could contain RPKM/FPKM ?

This kind of data could be used for most HD data analyses. The whole implementation is cloned from mRNA one.

The data examples could be found [here](https://github.com/transmart/tranSMART-ETL/tree/master/Kettle-GPL/data).

Below are production tables that are used for this implementation:
`deapp.de_rnaseq_annotation` - kontains "platform" information (transcriptoms).
`deapp.de_subject_rna_data` - contains measurements (RPKM/FPKM ?).

The ETL kettle scripts could be found [here](https://github.com/thehyve/tranSMART-ETL/tree/master/Kettle-GPL/Kettle-ETL).
See `load_rnaseq_annotation.kjb` for annotation upload.

Stored procedures that are used by kettle scripts to pump data from temp. tables to prodcution ones.
`tm_cz.i2b2_process_rna_seq_data`
`tm_cz.i2b2_rnaseq_inc_zscore_calc` - used by prev. st. proc. to calculate zscores.


