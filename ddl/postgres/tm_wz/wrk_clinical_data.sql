--
-- Name: wrk_clinical_data; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wrk_clinical_data (
    study_id character varying(25),
    site_id character varying(50),
    subject_id character varying(20),
    visit_name character varying(100),
    data_label character varying(500),
    data_value character varying(500),
    category_cd character varying(250),
    etl_job_id bigint,
    etl_date timestamp without time zone,
    usubjid character varying(200),
    category_path character varying(1000),
    data_type character varying(10),
    ctrl_vocab_code character varying(200)
);

