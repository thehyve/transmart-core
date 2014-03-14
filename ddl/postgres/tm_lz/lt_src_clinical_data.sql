--
-- Name: lt_src_clinical_data; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_src_clinical_data (
    study_id        character varying(25),
    site_id         character varying(50),
    subject_id      character varying(30),
    visit_name      character varying(100),
    data_label      character varying(500),
    modifier_cd     character varying(100),
    data_value      character varying(500),
    units_cd        character varying(50),
    date_timestamp  timestamp without time zone,
    category_cd     character varying(250),
    ctrl_vocab_code character varying(200)
);

