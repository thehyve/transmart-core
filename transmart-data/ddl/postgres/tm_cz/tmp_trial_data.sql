--
-- Name: tmp_trial_data; Type: TABLE; Schema: tm_cz; Owner: -
--
CREATE TABLE tmp_trial_data (
    usubjid character varying(50),
    study_id character varying(25),
    data_type character(1),
    visit_name character varying(100),
    data_label character varying(500),
    data_value character varying(500),
    unit_cd character varying(50),
    category_path character varying(250),
    sub_category_path_1 character varying(250),
    sub_category_path_2 character varying(250),
    patient_num bigint,
    sourcesystem_cd character varying(50),
    base_path character varying(1250)
);

