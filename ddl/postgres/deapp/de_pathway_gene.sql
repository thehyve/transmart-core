--
-- Name: de_pathway_gene; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_pathway_gene (
    id bigint NOT NULL,
    pathway_id bigint,
    gene_symbol character varying(200),
    gene_id character varying(200)
);

