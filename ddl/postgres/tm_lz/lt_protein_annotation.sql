--
-- Name: lt_protein_annotation; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_protein_annotation (
    gpl_id character varying(100),
    peptide character varying(200),
    uniprot_id character varying(200),
    biomarker_id numeric,
    organism character varying(100)
);

