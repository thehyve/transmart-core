--
-- Name: wt_subject_rbm_med; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_rbm_med (
    probeset_id character varying(1000),
    intensity_value bigint,
    log_intensity bigint,
    assay_id bigint,
    patient_id bigint,
    sample_id bigint,
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    pvalue double precision,
    num_calls bigint,
    mean_intensity bigint,
    stddev_intensity bigint,
    median_intensity bigint,
    zscore bigint
);

