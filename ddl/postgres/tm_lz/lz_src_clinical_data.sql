--
-- Name: lz_src_clinical_data; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lz_src_clinical_data (
    study_id        character varying(25),
    site_id         character varying(50),
    subject_id      character varying(30),
    visit_name      character varying(100),
    sample_type     character varying(100),
    data_label      character varying(500),
    data_value      character varying(500),
    modifier_cd     character varying(100),
    units_cd        character varying(50),
    category_cd     character varying(250),
    ctrl_vocab_code character varying(200),
    date_timestamp  timestamp without time zone,
    visit_date      character varying(200), --oracle
    etl_job_id      bigint,
    etl_date        timestamp without time zone
);

