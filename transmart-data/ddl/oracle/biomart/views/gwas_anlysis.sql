--
-- Type: VIEW; Owner: BIOMART; Name: GWAS_ANLYSIS
--
CREATE OR REPLACE FORCE VIEW "BIOMART"."GWAS_ANLYSIS" ("BIO_ASSAY_ANALYSIS_ID", "ETL_ID", "ANALYSIS_NAME", "BIO_ASSAY_DATA_TYPE") AS 
SELECT  to_char(bio_assay_analysis_id) bio_assay_analysis_id, etl_id, analysis_name, bio_assay_data_type from biomart.bio_assay_analysis where bio_assay_data_type not in ('Gene Expression');
