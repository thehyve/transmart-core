--
-- Name: wt_subject_mrna_data; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_mrna_data (
    probeset character varying(500),
    expr_id character varying(500),
    intensity_value numeric,
    assay_id bigint,
    patient_id bigint,
    sample_id bigint,
    subject_id character varying(100),
    trial_name character varying(200),
    timepoint character varying(200),
    sample_type character varying(200),
    platform character varying(200),
    tissue_type character varying(200)
);

