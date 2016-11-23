--
-- new table 
--

set search_path = biomart, pg_catalog;

\i ../../../ddl/postgres/biomart/bio_asy_analysis_data_ext.sql

ALTER TABLE bio_asy_analysis_data_ext OWNER TO biomart, SET TABLESPACE transmart;

-- ALTER INDEX bio_asy_analysis_data_ext_fk SET TABLESPACE indx;

-- index owner is always the same as the table owner

