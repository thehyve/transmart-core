--
-- Name: de_subject_microarray_data_new; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_microarray_data_new (
    trial_source character varying(200),
    trial_name character varying(50),
    probeset_id bigint,
    assay_id bigint,
    patient_id bigint,
    raw_intensity double precision,
    log_intensity double precision,
    zscore double precision
);

