--
-- Name: deapp_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE deapp_annotation (
    annotation_type character varying(50),
    annotation_value character varying(100),
    gene_id bigint,
    gene_symbol character varying(200)
);

