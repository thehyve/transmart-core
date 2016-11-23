--
-- change column owner to tm_cz for GWAS ETL functions to work
--

set search_path = biomart, pg_catalog;

-- Set owner to tm_cz
-- One must be owner to create child tables and indexes
-- within functions

ALTER TABLE biomart.bio_assay_analysis_gwas OWNER To tm_cz;

-- If the table owner does not match the schema owner, assign all permissions to
-- the schema owner.

GRANT ALL ON TABLE biomart.bio_assay_analysis_gwas TO biomart;
