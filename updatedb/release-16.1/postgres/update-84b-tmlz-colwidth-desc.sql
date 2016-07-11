--
-- Increase column width for analysis description for GWAS example data
-- old size 500
-- new size 2048
--

set search_path = tm_lz, pg_catalog;

ALTER TABLE ONLY tm_lz.lz_src_analysis_metadata ALTER COLUMN description TYPE character varying(2048);
