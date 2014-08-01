--
-- Name: de_rc_snp_info1; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rc_snp_info1 (
    snp_info_id bigint DEFAULT nextval('de_rc_snp_info_seq'::regclass) NOT NULL,
    rs_id character varying(50),
    chrom character varying(4),
    pos bigint,
    hg_version character varying(10),
    exon_intron character varying(10),
    recombination_rate numeric(18,6),
    recombination_map numeric(18,6),
    regulome_score character varying(10),
    gene_name character varying(50),
    entrez_id character varying(50)
);

