--
-- Name: lt_src_rbm_subj_samp_map; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_src_rbm_subj_samp_map (
    trial_name character varying(100),
    site_id character varying(100),
    subject_id character varying(100),
    sample_cd character varying(100),
    platform character varying(100),
    tissue_type character varying(100),
    attribute_1 character varying(256),
    attribute_2 character varying(200),
    category_cd character varying(200),
    source_cd character varying(200)
);

