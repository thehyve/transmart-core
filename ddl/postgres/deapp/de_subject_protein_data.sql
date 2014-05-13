--
-- Name: de_subject_protein_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_protein_data (
    trial_name character varying(50),
    component character varying(200),
    intensity bigint,
    patient_id bigint,
    subject_id character varying(10),
    gene_symbol character varying(100),
    gene_id integer,
    assay_id bigint,
    timepoint character varying(20),
    n_value bigint,
    mean_intensity bigint,
    stddev_intensity bigint,
    median_intensity bigint,
    zscore bigint,
    log_intensity bigint,
    protein_annotation_id bigint
);

