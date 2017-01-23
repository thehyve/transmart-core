--
-- bugfixes for Sanofi test studies data ETL
--

set search_path = tm_cz, pg_catalog;

DROP FUNCTION IF EXISTS tm_cz.i2b2_load_metabolomics_annot(numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_load_metabolomics_annot.sql

ALTER FUNCTION i2b2_load_metabolomics_annot(numeric) SET search_path TO tm_cz, i2b2metadata, pg_temp, deapp, tm_lz, tm_wz, biomart;
