--
-- Name: wt_subject_proteomics_logs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_proteomics_logs (
    probeset_id character varying(500),
    intensity_value bigint,
    pvalue double precision,
    num_calls bigint,
    assay_id bigint,
    patient_id bigint,
    sample_id bigint,
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    log_intensity bigint
);

