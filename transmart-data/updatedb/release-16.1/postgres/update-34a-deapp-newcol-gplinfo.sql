--
-- add columns to deapp.de_gpl_info for chromosome region
--

set search_path = deapp, pg_catalog;

ALTER TABLE deapp.de_gpl_info
    ADD COLUMN gene_annotation_id character varying(50),
    ADD COLUMN chromosome character varying(5),
    ADD COLUMN start_bp bigint,
    ADD COLUMN end_bp bigint
;

