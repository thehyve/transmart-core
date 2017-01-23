--
-- Name: wrk_clinical_data; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wrk_clinical_data (
    study_id character varying(25),
    site_id character varying(50),
    subject_id character varying(100),
    visit_name character varying(100),
    data_label character varying(500),
    data_value character varying(500),
    category_cd character varying(250),
    etl_job_id numeric(22,0),
    etl_date timestamp without time zone,
    usubjid character varying(200),
    category_path character varying(1000),
    data_type character varying(10),
    data_label_ctrl_vocab_code character varying(200),
    data_value_ctrl_vocab_code character varying(500),
    data_label_components character varying(1000),
    units_cd character varying(50),
    visit_date character varying(200),
    node_name character varying(1000),
    link_type character varying(20),
    link_value character varying(200),
    end_date character varying(50),
    visit_reference character varying(100),
    date_ind character(1),
    obs_string character varying(100),
    rec_num numeric(18,0),
    valuetype_cd character varying(50),
    leaf_node character varying(2000),
    modifier_cd character varying(100),
    ctrl_vocab_code character varying(200),
    date_timestamp timestamp without time zone,
    sample_cd character varying(200)
);

