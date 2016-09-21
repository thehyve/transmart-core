--
-- Name: wt_subject_rnaseq_region; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE tm_wz.wt_subject_rnaseq_region
(
    region_id bigint,
    expr_id character varying(500),
    readcount bigint,
    normalized_readcount double precision,
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

