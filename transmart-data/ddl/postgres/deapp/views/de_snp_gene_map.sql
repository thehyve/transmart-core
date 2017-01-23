--
-- Name: de_snp_gene_map; Type: VIEW; Schema: deapp; Owner: -
--
CREATE VIEW de_snp_gene_map AS
 SELECT de_rc_snp_info.snp_info_id AS snp_id,
    de_rc_snp_info.rs_id AS snp_name,
    de_rc_snp_info.entrez_id AS entrez_gene_id,
    de_rc_snp_info.gene_name
   FROM de_rc_snp_info
  WHERE ((de_rc_snp_info.hg_version)::numeric = (19)::numeric);

