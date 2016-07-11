--
-- cleanup source code whitespace
--

set search_path = tm_cz, pg_catalog;

DROP FUNCTION IF EXISTS tm_cz.i2b2_load_clinical_data(character varying, character varying, character varying, character varying, numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_load_clinical_data.sql

ALTER FUNCTION i2b2_load_clinical_data(character varying, character varying, character varying, character varying, numeric) SET search_path TO tm_cz, tm_lz, tm_wz, i2b2demodata, i2b2metadata, deapp, pg_temp;
