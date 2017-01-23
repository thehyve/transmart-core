--
-- Name: bio_asy_analysis_gwas_top50; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE bio_asy_analysis_gwas_top50 (
    bio_assay_analysis_id bigint,
    analysis character varying(500),
    chrom character varying(4),
    pos numeric(10,0),
    rsgene character varying(200),
    rsid character varying(50),
    pvalue double precision,
    logpvalue double precision,
    extdata character varying(4000),
    rnum bigint,
    intronexon character varying(10),
    regulome character varying(10),
    recombinationrate numeric(18,6)
);
