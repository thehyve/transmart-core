--
-- Name: de_subject_mrna_data_release; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE de_subject_mrna_data_release (
    trial_name character varying(50),
    probeset_id bigint,
    assay_id bigint,
    patient_id bigint,
    timepoint character varying(100),
    pvalue double precision,
    refseq character varying(50),
    subject_id character varying(50),
    raw_intensity bigint,
    mean_intensity double precision,
    stddev_intensity double precision,
    median_intensity double precision,
    log_intensity double precision,
    zscore double precision,
    sample_id bigint,
    release_study character varying(50)
);

