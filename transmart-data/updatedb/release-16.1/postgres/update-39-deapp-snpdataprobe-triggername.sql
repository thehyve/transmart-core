--
-- rename trigger to fix typo in name
--

set search_path = deapp, pg_catalog;

ALTER TRIGGER trg_snp_data_by_pprobe_id ON de_snp_data_by_probe RENAME TO trg_snp_data_by_probe_id;
