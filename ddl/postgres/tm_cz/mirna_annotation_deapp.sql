--
-- Name: mirna_annotation_deapp; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE mirna_annotation_deapp (
    id_ref character varying(100),
    probe_id character varying(100),
    mirna_symbol character varying(100),
    mirna_id character varying(100),
    probeset_id bigint,
    organism character varying(200)
);

