--
-- set primary key for table biomart.bio_assay_analysis_gwas
--

set search_path = biomart, pg_catalog;

ALTER TABLE bio_assay_analysis_gwas ALTER COLUMN bio_asy_analysis_gwas_id SET NOT NULL;


ALTER TABLE ONLY bio_assay_analysis_gwas
    ADD CONSTRAINT bio_asy_analysis_gwas_id PRIMARY KEY (bio_asy_analysis_gwas_id);

ALTER INDEX bio_asy_analysis_gwas_id SET TABLESPACE indx;

-- index owner is always the same as the table owner
