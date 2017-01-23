--
-- Increase column width for clinical subject ID
-- old size 20
-- new size 100 for consistency with other tables (30 in release 16.1)
--

set search_path = tm_wz, pg_catalog;

ALTER TABLE ONLY tm_wz.wrk_clinical_data ALTER COLUMN subject_id TYPE character varying(100);

-- duplicate definition may not exist
ALTER TABLE IF EXISTS ONLY tm_wz.wt_clinical_data_dups ALTER COLUMN subject_id TYPE character varying(100);

