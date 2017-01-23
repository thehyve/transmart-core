--
-- check for null values
--

set search_path = tm_cz, pg_catalog;

DROP FUNCTION IF EXISTS tm_cz.i2b2_load_annotation_deapp(numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_load_annotation_deapp.sql

ALTER FUNCTION i2b2_load_annotation_deapp(numeric) SET search_path TO tm_cz, deapp, biomart, pg_temp;
