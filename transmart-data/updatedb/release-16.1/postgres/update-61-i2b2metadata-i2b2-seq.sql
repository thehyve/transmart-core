--
-- drop record_id column from table i2b2
--

set search_path = i2b2metadata, pg_catalog;

DROP INDEX i2b2metadata.i2b2meta_idx_record_id;
DROP INDEX idx_i2b2_basecode;

ALTER TABLE i2b2metadata.i2b2 DROP COLUMN record_id;

DROP SEQUENCE i2b2metadata.i2b2_record_id_seq;
