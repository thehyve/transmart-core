--
-- Name: lt_snp_copy_number; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_snp_copy_number ( --UNLOGGED
    gsm_num character varying(10),
    snp_name character varying(50),
    chrom character varying(2),
    chrom_pos numeric(20,0),
    copy_number double precision
);

