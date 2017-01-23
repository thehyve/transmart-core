--
-- Name: de_subj_protein_data_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE de_subj_protein_data_release (
    trial_name character varying(15),
    component character varying(15),
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
    release_study character varying(15)
);

