--
-- rename index to correct typo in name
--

set search_path = biomart, pg_catalog;

ALTER INDEX baad_idx_tea_enalysis RENAME TO baad_idx_tea_analysis;
