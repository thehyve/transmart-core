--
-- rename trigger tf_trg_snp_data_by_pprobe_id to correct typo
--

set search_path = deapp, pg_catalog;

ALTER FUNCTION tf_trg_snp_data_by_pprobe_id() RENAME TO tf_trg_snp_data_by_probe_id;

