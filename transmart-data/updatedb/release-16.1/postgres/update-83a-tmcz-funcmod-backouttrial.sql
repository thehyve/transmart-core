--
-- improvements to i2b2_backout_trial code
--

set search_path = tm_cz, pg_catalog;

DROP FUNCTION IF EXISTS tm_cz.i2b2_backout_trial(character varying, character varying, numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_backout_trial.sql

ALTER FUNCTION i2b2_backout_trial(character varying, character varying, numeric) SET search_path TO tm_cz, i2b2metadata, i2b2demodata, pg_temp;
