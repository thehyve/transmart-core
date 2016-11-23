--
-- Name: lt_src_clinical_data; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_src_clinical_data (
    study_id character varying(25),
    site_id character varying(50),
    subject_id character varying(100),
    visit_name character varying(100),
    sample_type character varying(100),
    data_label character varying(500),
    data_value character varying(500),
    category_cd character varying(250),
    data_label_ctrl_vocab_code character varying(200),
    data_value_ctrl_vocab_code character varying(500),
    data_label_components character varying(1000),
    units_cd character varying(50),
    visit_date character varying(200),
    link_type character varying(20),
    link_value character varying(200),
    end_date character varying(50),
    visit_reference character varying(100),
    date_ind character(1),
    obs_string character varying(100),
    valuetype_cd character varying(50),
    date_timestamp timestamp without time zone,
    ctrl_vocab_code character varying(200),
    modifier_cd character varying(100),
    sample_cd character varying(200)
);

