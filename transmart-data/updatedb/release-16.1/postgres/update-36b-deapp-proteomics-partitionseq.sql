--
-- update sequence for proteomics partitions
--

set search_path = deapp, pg_catalog;

ALTER SEQUENCE deapp.seq_proteomics_partition_id OWNER TO deapp;
