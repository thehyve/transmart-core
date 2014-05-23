--
-- Name: wt_subject_mirna_med; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_mirna_med (
    probeset_id character varying(1000), --was numeric(38,0) but char in postgres
    intensity_value numeric,
    log_intensity numeric,
    assay_id numeric(18,0),
    patient_id numeric(18,0),
    sample_id numeric(18,0),
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    pvalue double precision,
    num_calls numeric,
    mean_intensity numeric,
    stddev_intensity numeric,
    median_intensity numeric,
    zscore numeric
);

