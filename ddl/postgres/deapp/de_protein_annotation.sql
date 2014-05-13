--
-- Name: de_protein_annotation; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_protein_annotation (
    id bigint,
    gpl_id character varying(50) NOT NULL,
    peptide character varying(200) NOT NULL,
    uniprot_id character varying(50),
    biomarker_id character varying(200),
    organism character varying(200),
    uniprot_name character varying(200),
    PRIMARY KEY (id)
);
