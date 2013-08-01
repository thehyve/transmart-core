--
-- Name: de_snp_copy_number; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_snp_copy_number (
    patient_num bigint,
    snp_name character varying(50),
    chrom character varying(2),
    chrom_pos bigint,
    copy_number smallint
);

