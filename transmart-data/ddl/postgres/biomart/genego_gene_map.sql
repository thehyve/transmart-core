--
-- Name: genego_gene_map; Type: TABLE; Schema: biomart; Owner: -
--
CREATE TABLE genego_gene_map (
    gene_symbol character varying(20),
    gene_id character varying(10),
    bio_marker_id bigint NOT NULL
);

