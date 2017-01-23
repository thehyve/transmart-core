--
-- function for incremental clinical loading
--

set search_path = tm_cz, pg_catalog;

DROP FUNCTION IF EXISTS i2b2_create_security_inc_trial(character varying, character varying, numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_create_security_inc_trial.sql

ALTER FUNCTION i2b2_create_security_inc_trial(character varying, character varying, numeric) SET search_path TO tm_cz, i2b2demodata, i2b2metadata, searchapp, biomart, pg_temp;

