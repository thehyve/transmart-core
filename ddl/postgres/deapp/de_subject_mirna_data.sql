--
-- Name: de_subject_mirna_data; Type: TABLE; Schema: deapp; Owner: -
--
CREATE TABLE de_subject_mirna_data (
    trial_source character varying(200),
    trial_name character varying(50),
    assay_id bigint,
    patient_id bigint,
    raw_intensity double precision,
    log_intensity double precision,
    zscore double precision,
    probeset_id bigint
);
