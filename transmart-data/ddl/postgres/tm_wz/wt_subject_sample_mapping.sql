--
-- Name: wt_subject_sample_mapping; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_subject_sample_mapping (
    patient_num numeric(38,0),
    site_id character varying(100),
    subject_id character varying(100),
    concept_code character varying(50),
    sample_type character varying(100),
    sample_type_cd character varying(100),
    timepoint character varying(100),
    timepoint_cd character varying(50),
    tissue_type character varying(100),
    tissue_type_cd character varying(50),
    platform character varying(50),
    platform_cd character varying(50),
    data_uid character varying(100),
    gpl_id character varying(20),
    sample_cd character varying(200),
    category_cd character varying(1000)
);

