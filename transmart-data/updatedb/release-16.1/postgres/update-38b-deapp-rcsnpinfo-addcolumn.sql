--
-- wider column variation_class for deapp.de_rc_snp_info
-- old size 10
-- new size 24
--

set search_path = deapp, pg_catalog;

ALTER TABLE ONLY deapp.de_rc_snp_info ALTER COLUMN variation_class TYPE character varying(24);
