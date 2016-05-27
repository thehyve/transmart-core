--
-- typos in audit log messages
--

set search_path = tm_cz, pg_catalog;
SET check_function_bodies = false;

DROP FUNCTION IF EXISTS tm_cz.i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, character varying, numeric, character varying, numeric);
DROP FUNCTION IF EXISTS tm_cz.i2b2_metabolomics_zscore_calc(character varying, character varying, character varying, numeric, character varying, numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_metabolomics_zscore_calc.sql

-- No ALTER FUNCTION for search path defined
