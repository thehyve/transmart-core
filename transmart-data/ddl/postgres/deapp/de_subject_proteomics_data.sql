--
-- Name: de_subject_proteomics_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_proteomics_data (
    trial_name character varying(15),
    protein_annotation_id bigint,
    component character varying(100),
    patient_id bigint,
    gene_symbol character varying(100),
    gene_id bigint,
    assay_id bigint,
    subject_id character varying(100),
    intensity bigint,
    zscore bigint,
    partition_id numeric
);

