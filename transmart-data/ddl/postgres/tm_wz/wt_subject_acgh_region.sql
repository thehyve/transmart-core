--
-- Name: wt_subject_acgh_region; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_acgh_region (
    region_id bigint,
    expr_id character varying(500),
    chip double precision,
    segmented double precision,
    flag smallint,
    probhomloss double precision,
    probloss double precision,
    probnorm double precision,
    probgain double precision,
    probamp double precision,
    num_calls bigint,
    pvalue double precision,
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

