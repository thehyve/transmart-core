--
-- Name: de_subject_microarray_med; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_microarray_med (
    probeset character varying(50),
    raw_intensity bigint,
    log_intensity bigint,
    gene_symbol character varying(50),
    assay_id bigint,
    patient_id bigint,
    subject_id character varying(20),
    trial_name character varying(15),
    timepoint character varying(30),
    pvalue double precision,
    refseq character varying(50),
    mean_intensity bigint,
    stddev_intensity bigint,
    median_intensity bigint,
    zscore bigint
);

