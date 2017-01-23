--
-- Name: de_rnaseq_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_rnaseq_annotation (
    gpl_id character varying(50),
    transcript_id character varying(50),
    gene_symbol character varying(50),
    gene_id character varying(50),
    organism character varying(30),
    probeset_id bigint
);

