--
-- Name: lt_src_omicsoft_data; Type: TABLE; Schema: tm_lz; Owner: -
--
CREATE TABLE lt_src_omicsoft_data (
    id character varying(1000),
    contrast_name character varying(1000),
    probe_id character varying(1000),
    raw_p_value character varying(200),
    adj_p_value character varying(200),
    estimate double precision,
    fold_change double precision,
    max_l_s_mean double precision,
    bio_assay_analysis_id bigint,
    mean_fold_change double precision,
    std_dev_fold_change double precision,
    tea_normal_pvalue double precision,
    etl_id character varying(200)
);

