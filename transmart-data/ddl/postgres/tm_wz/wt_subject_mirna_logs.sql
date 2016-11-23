--
-- Name: wt_subject_mirna_logs; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_mirna_logs (
    probeset_id character varying(1000), -- was numeric(38,0) in postgres
    intensity_value numeric,
    pvalue double precision,
    num_calls numeric,
    assay_id numeric(18,0),
    patient_id numeric(18,0),
    sample_id numeric(18,0),
    subject_id character varying(50),
    trial_name character varying(50),
    timepoint character varying(100),
    log_intensity numeric
);

