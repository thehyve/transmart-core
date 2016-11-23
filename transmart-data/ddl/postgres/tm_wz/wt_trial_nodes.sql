--
-- Name: wt_trial_nodes; Type: TABLE; Schema: tm_wz; Owner: -
--
CREATE TABLE wt_trial_nodes (
    leaf_node character varying(4000),
    category_cd character varying(250),
    visit_name character varying(100),
    sample_type character varying(100),
    data_label character varying(500),
    node_name character varying(500),
    data_value character varying(500),
    data_type character varying(20),
    data_label_ctrl_vocab_code character varying(500),
    data_value_ctrl_vocab_code character varying(500),
    data_label_components character varying(1000),
    link_type character varying(50),
    obs_string character varying(100),
    valuetype_cd character varying(50),
    rec_num numeric
);

