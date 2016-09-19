--
-- add column to de_subject_rnaseq_data
--

set search_path = deapp, pg_catalog;

ALTER TABLE deapp.de_subject_rnaseq_data ADD COLUMN trial_source character varying(200);

