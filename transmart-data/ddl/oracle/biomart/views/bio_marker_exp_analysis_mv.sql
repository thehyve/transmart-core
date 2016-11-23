--
-- Type: VIEW; Owner: BIOMART; Name: BIO_MARKER_EXP_ANALYSIS_MV
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."BIO_MARKER_EXP_ANALYSIS_MV" ("BIO_MARKER_ID", "BIO_EXPERIMENT_ID", "BIO_ASSAY_ANALYSIS_ID", "MV_ID") AS 
  SELECT DISTINCT t4.bio_marker_id,
  t1.bio_experiment_id,
  t1.bio_assay_analysis_id,
  t1.bio_assay_analysis_id*100+t4.bio_marker_id
FROM BIOMART.BIO_ASSAY_ANALYSIS_DATA t1,
  BIOMART.BIO_EXPERIMENT t2,
  DEAPP.DE_MRNA_ANNOTATION t3,
  BIOMART.BIO_MARKER t4
WHERE t1.bio_experiment_id       = t2.bio_experiment_id
AND t2.bio_experiment_type       ='Experiment'
AND t3.probeset_id= t1.probeset_id
AND t4.primary_external_id = CAST(t3.gene_id AS VARCHAR(200)); 
