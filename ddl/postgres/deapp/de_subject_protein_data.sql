--
-- Name: de_subject_protein_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_protein_data (
    trial_name character varying(15),
    component character varying(15),
    intensity numeric,
    patient_id bigint,
    subject_id character varying(10),
    gene_symbol character varying(100),
    gene_id integer,
    assay_id bigint,
    timepoint character varying(20),
    n_value bigint,
    mean_intensity numeric,
    stddev_intensity numeric,
    median_intensity numeric,
    zscore double precision
);

