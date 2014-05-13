--
-- Name: wt_subject_microarray_med; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_microarray_med (
    probeset_id bigint,
    intensity_value numeric,
    log_intensity numeric,
    assay_id bigint,
    patient_id bigint,
    sample_id bigint,
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    pvalue double precision,
    num_calls bigint,
    mean_intensity numeric,
    stddev_intensity numeric,
    median_intensity numeric,
    zscore double precision
);

