--
-- Name: annotation_deapp; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE annotation_deapp (
    gpl_id character varying(100),
    probe_id character varying(100),
    gene_symbol character varying(100),
    gene_id character varying(100),
    probeset_id bigint,
    organism character varying(200)
);

