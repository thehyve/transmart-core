--
-- Name: de_snp_info_hg19_mv; Type: VIEW; Schema: deapp; Owner: -
--
CREATE VIEW de_snp_info_hg19_mv AS
 SELECT info.rs_id,
    info.chrom,
    info.pos,
    info.gene_name AS rsgene,
    info.exon_intron,
    info.recombination_rate,
    info.regulome_score
   FROM de_rc_snp_info info
  WHERE ((info.hg_version)::text = '19'::text);

