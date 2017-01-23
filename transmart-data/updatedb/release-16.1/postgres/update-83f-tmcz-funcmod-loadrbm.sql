--
-- bugfixes for Sanofi test studies data ETL
--

set search_path = tm_cz, pg_catalog;

DROP FUNCTION IF EXISTS tm_cz.i2b2_load_rbm_data(character varying, character varying, character varying, character varying, numeric, character varying, numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_load_rbm_data.sql

-- no ALTER FUNCTION needed for path for load data functions
