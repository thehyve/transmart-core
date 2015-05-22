--
-- Name: wt_clinical_data_dups; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_clinical_data_dups (
    site_id     character varying(50),
    subject_id  character varying(30),
    visit_name  character varying(100),
    data_label  character varying(500),
    category_cd character varying(250),
    modifier_cd character varying(100),
    link_value character varying(500)
);

