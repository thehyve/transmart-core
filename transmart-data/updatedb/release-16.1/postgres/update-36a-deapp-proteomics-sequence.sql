--
-- add sequence for proteomics partitions
--

set search_path = deapp, pg_catalog;

CREATE SEQUENCE deapp.seq_proteomics_partition_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

GRANT ALL ON SEQUENCE deapp.seq_proteomics_partition_id TO biomart_user;
