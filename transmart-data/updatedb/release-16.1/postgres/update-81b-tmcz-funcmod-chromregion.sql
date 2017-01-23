--
-- use new column gene_annotationId column in de_gpl_info
--

set search_path = tm_cz, pg_catalog;

-- replacing by function with 1 extra parameter

DROP FUNCTION IF EXISTS i2b2_load_chrom_region(character varying,character varying,character varying,numeric);
DROP FUNCTION IF EXISTS i2b2_load_chrom_region(character varying,character varying,character varying,character varying,numeric);

\i ../../../ddl/postgres/tm_cz/functions/i2b2_load_chrom_region.sql

ALTER FUNCTION i2b2_load_chrom_region(character varying, character varying, character varying, character varying, numeric) SET search_path TO tm_cz, deapp;

