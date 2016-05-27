--
-- Increase column width for clinical subject ID
-- old size 20
-- new size 100
--

set search_path = tm_lz, pg_catalog;

ALTER TABLE ONLY tm_lz.lt_src_clinical_data ALTER COLUMN subject_id TYPE character varying(100);

ALTER TABLE ONLY tm_lz.lz_src_clinical_data ALTER COLUMN subject_id TYPE character varying(100);
